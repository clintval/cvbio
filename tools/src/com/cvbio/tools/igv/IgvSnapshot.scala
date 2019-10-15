package com.cvbio.tools.igv

import com.cvbio.commons.CommonsDef._
import com.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import com.cvbio.tools.igv.Igv._
import com.fulcrumgenomics.sopt._
import htsjdk.samtools.util.Interval

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
  @arg(flag = 'c', doc = "The contig to \"goto\".") val contig: String,
  @arg(flag = 's', doc = "The start position to \"goto\".") val start: Int,
  @arg(flag = 'e', doc = "The end position to \"goto\".") val end: Int,
  @arg(flag = 'o', doc = "Output path to a rendered image (.png, .jpg, or .svg).") val output: Option[FilePath] = None,
  @arg(flag = 'H', doc = "The host the IGV server is running on.") val host: String = DefaultHost,
  @arg(flag = 'p', doc = "The port to the IGV server.") val port: Int = DefaultPort,
) extends CvBioTool {

  output.foreach(validateOutput)

  override def execute(): Unit = {
    val play = new IgvPlay()
    val igv  = new Igv(host, port)

    play += New
    play += Load(input)
    play += Goto(new Interval(contig, start, end))

    output.foreach(path => play += Snapshot(path.toAbsolutePath))

    igv.runPlay(play)
    igv.close()
  }

  /** Validate the output file path to ensure it ends with the correct extension. */
  private def validateOutput(output: FilePath): Unit = {
    lazy val message = s"Output must have one of ${ValidOutputSuffixes.mkString(", ")} file extensions. Found: $output"
    validate(ValidOutputSuffixes.exists(output.toString.endsWith), message = message)
  }
}
