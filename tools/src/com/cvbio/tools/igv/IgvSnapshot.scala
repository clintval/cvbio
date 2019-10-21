package com.cvbio.tools.igv

import com.cvbio.commons.CommonsDef._
import com.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import com.cvbio.tools.igv.Igv._
import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.commons.io.PathUtil.{basename, sanitizeFileName}
import com.fulcrumgenomics.sopt._

@clp(
  description =
    """
      |Take screenshots in IGV automatically!
      |
      |## References and Prior Art
      |
      |  - https://software.broadinstitute.org/software/igv/PortCommands
      |  - https://github.com/stevekm/IGV-snapshot-automator
    """,
  group = ClpGroups.Igv
) class IgvSnapshot(
  @arg(flag = 'i', doc = "Input files to display.", minElements = 1) val input: Seq[FilePath],
  @arg(flag = 'l', doc = "The loci to take snapshots over. (e.g. \"all\", \"chr1:23-99\", \"TP53\").", minElements = 0) val loci: Seq[String] = Seq.empty,
  // @arg(flag = 'o', doc = "Output path prefix to the rendered images.") val output: Option[PathPrefix] = None,
  // @arg(flag = 'f', doc = "The output snapshot format") val format: OutputFormat = OutputFormat.Svg,
  @arg(flag = 'j', doc = "The IGV Jar file, if we are to initialize IGV ourselves.") val jar: Option[FilePath] = None,
  @arg(flag = 'm', doc = "The memory, in megabytes, given to IGV, if we are to initialize.") val memory: Int = DefaultMemory,
  @arg(flag = 'H', doc = "The host the IGV server is running on.") val host: String = DefaultHost,
  @arg(flag = 'p', doc = "The port to the IGV server.") val port: Int = DefaultPort,
  @arg(flag = 'x', doc = "Close the IGV application after execution") val closeOnExit: Boolean = false,
) extends CvBioTool {

  override def execute(): Unit = {
    val igv  = jar match {
      case Some(_jar) => Igv(_jar, host, port, memory, closeOnExit)
      case None       => Igv(Igv.Executable, host, port, closeOnExit)
    }

    val play = new IgvPlay() += New += Load(input)
    if (loci.nonEmpty) { play += Goto(loci) }

    igv.runPlay(play)
  }
}