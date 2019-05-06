package com.cvbio.ensembl


import java.io.{BufferedReader, InputStreamReader}
import java.nio.file.Path
import java.util.zip.GZIPInputStream

import com.cvbio.cmdline.{ClpGroups, CvBioTool}
import com.fulcrumgenomics.commons.io.Io
import com.fulcrumgenomics.commons.util.LazyLogging
import com.fulcrumgenomics.sopt._
import org.apache.http.client.utils.URIBuilder
import sun.net.www.protocol.ftp.FtpURLConnection

import scala.util.{Failure, Success, Try}

@clp(
  description =
    """
      |Download a GTF file from the Ensembl web server.
    """,
  group = ClpGroups.Ensembl
) class DownloadGtf(
  @arg(flag = 'r', doc = "The Ensembl release.") val release: Int = 96,
  @arg(flag = 'b', doc = "The genome build.") val build: Int = 38,
  @arg(flag = 's', doc = "The species.") val species: String = "Homo sapiens",
  @arg(flag = 'o', doc = "The output file path.") val out: Path = Io.StdOut
) extends CvBioTool
  with LazyLogging {

  private val ConnectionTimeout   = 5000
  private val GzipInputBufferSize = 4096
  private val ReadTimeout         = 5000

  override def execute(): Unit = {
    Io.assertCanWriteFile(out)

    val speciesFmt  = species.replaceAll("\\s+", "_").toLowerCase
    val filename    = s"${speciesFmt.capitalize}.GRCh$build.$release.gtf.gz"
    val filepath    = s"/pub/release-$release/gtf/$speciesFmt/$filename"

    val url = new URIBuilder()
      .setScheme("ftp")
      .setHost(EnsemblDef.FtpHost)
      .setPath(filepath)
      .build
      .toURL

    val connection  = new FtpURLConnection(url)
    val writer      = Io.toWriter(out)

    connection.setConnectTimeout(ConnectionTimeout)
    connection.setReadTimeout(ReadTimeout)
    connection.connect()

    // TODO: Ensembl provides md5 checksums, it would be clever to "tee" the stream and validate the checksum!
    Try(connection.getInputStream) match {
      case Success(inputStream) =>
        logger.info(s"Streaming URL: $url")
        val gzipInputStream  = new GZIPInputStream(inputStream, GzipInputBufferSize)
        val inputStreamReader = new InputStreamReader(gzipInputStream)
        val bufferedReader    = new BufferedReader(inputStreamReader)

        Iterator
          .continually(bufferedReader.readLine())
          .takeWhile(_ != null)
          .foreach(line => writer.write(line + "\n"))

        bufferedReader.close()
      case Failure(e) =>
        logger.warning(s"Could not download resource: $url")
        throw e
    }

    connection.close()
    writer.close()
  }
}
