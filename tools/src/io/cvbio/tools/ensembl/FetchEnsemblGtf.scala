package io.cvbio.tools.ensembl

import java.io.{BufferedReader, InputStreamReader}
import java.nio.file.Path
import java.util.zip.GZIPInputStream

import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import com.fulcrumgenomics.util.Io
import com.fulcrumgenomics.sopt._
import org.apache.http.client.utils.URIBuilder
import sun.net.www.protocol.ftp.FtpURLConnection

import scala.util.{Failure, Success, Try}

@clp(
  description =
    """
      |Fetch a GTF file from the Ensembl web server.
    """,
  group  = ClpGroups.Util
) class FetchEnsemblGtf(
  @arg(flag = 'o', doc = "The output file path.") val output: Path,
  @arg(flag = 'r', doc = "The Ensembl release.") val release: Int = 96,
  @arg(flag = 'b', doc = "The genome build.") val build: Int = 38,
  @arg(flag = 's', doc = "The species.") val species: String = "Homo sapiens"
) extends CvBioTool {

  private val ConnectionTimeout = 5000
  private val GzipBufferSize    = 4096
  private val ReadTimeout       = 5000

  override def execute(): Unit = {
    Io.assertCanWriteFile(output)
    val writer = Io.toWriter(output)

    val speciesFmt = species.replaceAll("\\s+", "_").toLowerCase
    val filename   = s"${speciesFmt.capitalize}.GRCh$build.$release.gtf.gz"
    val filepath   = s"/pub/release-$release/gtf/$speciesFmt/$filename"

    val url = new URIBuilder()
      .setScheme("ftp")
      .setHost(EnsemblDef.FtpHost)
      .setPath(filepath)
      .build
      .toURL

    val connection = new FtpURLConnection(url)

    connection.setConnectTimeout(ConnectionTimeout)
    connection.setReadTimeout(ReadTimeout)
    connection.connect()

    // TODO: Ensembl provides md5 checksums, it would be clever to "tee" the stream and validate the checksum!
    Try(connection.getInputStream) match {
      case Success(inputStream) =>
        logger.info(s"Streaming URL: $url")
        val gzipInputStream = new GZIPInputStream(inputStream, GzipBufferSize)
        val streamReader    = new InputStreamReader(gzipInputStream)
        val bufferedReader  = new BufferedReader(streamReader)

        Iterator
          .continually(bufferedReader.readLine())
          .takeWhile(_ != null)
          .foreach(line => writer.write(s"$line\n"))

        bufferedReader.close()
      case Failure(e) =>
        logger.warning(s"Could not download resource: $url")
        throw e
    }

    connection.close()
    writer.close()
  }
}
