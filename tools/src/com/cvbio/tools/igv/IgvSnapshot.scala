package com.cvbio.tools.igv

import com.cvbio.commons.CommonsDef._
import com.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import com.fulcrumgenomics.sopt._
import htsjdk.samtools.util.Interval

@clp(
  description =
    """
      |Take screenshots in IGV automatically!
      |
      |## References
      |
      |  - https://software.broadinstitute.org/software/igv/PortCommands
      |  - https://github.com/stevekm/IGV-snapshot-automator
    """,
  group  = ClpGroups.Igv
) class IgvSnapshot(
  @arg(flag = 'i', doc = "Input files to display.", minElements = 1) val input: Seq[FilePath],
  @arg(flag = 'o', doc = "Output path to a rendered image (.png, .jpg, or .svg).") val output: Option[FilePath] = None,
  @arg(flag = 'l', doc = "The intervals to take snapshots over.") val intervals: Option[PathToIntervals] = None,
  @arg(flag = 'c', doc = "The contig to \"goto\".", mutex = Array("intervals")) val contig: Option[String] = None,
  @arg(flag = 's', doc = "The start position to \"goto\".", mutex = Array("intervals")) val start: Option[Int] = None,
  @arg(flag = 'e', doc = "The end position to \"goto\".", mutex = Array("intervals")) val end: Option[Int] = None,
  @arg(flag = 'H', doc = "The host the IGV server is running on.") val host: String = Igv.DefaultHost,
  @arg(flag = 'p', doc = "The port to the IGV server.") val port: Int = Igv.DefaultPort,
) extends CvBioTool {

  /** Valid output suffixes for snapshot file paths. */
  private val OutputSuffixes: Seq[FilenameSuffix] = Seq(".png", ".jpg", ".svg")

  output.foreach { path =>
    validate(
      OutputSuffixes.exists(path.toString.endsWith),
      message = s"Output must have one of ${OutputSuffixes.mkString(", ")} file extensions. Found: $path")
  }

  override def execute(): Unit = {
    val igv  = new Igv(host, port)
    val play = IgvPlay()

    play.add(New).add(Load(input))

    (contig, start, end, intervals) match {
      case (Some(_contig), Some(_start), Some(_end), None) => {
        play.add(Goto(new Interval(_contig, _start, _end)))
      }
      case (_, _, _, _) => Unit
    }

    output.foreach(path => play.add(Snapshot(path)))

    igv.runPlay(play)
  }
}
