package com.cvbio.tools.igv

import com.cvbio.commons.CommonsDef._
import com.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import com.fulcrumgenomics.commons.io.PathUtil
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
  @arg(flag = 'i', doc = "Input BAM.") val input: PathToBam = PathUtil.pathTo("/Users/cvalentine/Downloads/one-read-pair.sorted.bam"),
  @arg(flag = 'H', doc = "The host the IGV server is running on.") val host: String = "127.0.0.1",
  @arg(flag = 'p', doc = "The port to the IGV server.") val port: Int = 60151
) extends CvBioTool {

  override def execute(): Unit = {
    val igv      = new IgvController(host, port)
    val interval = new Interval("chr21", 6495564, 6496373)

    igv.exec(New)
    igv.exec(Echo)
    igv.exec(Load(input))
    igv.exec(Goto(interval))
    igv.exec(Region(interval))
    igv.exec(Snapshot(PathUtil.pathTo("/Users/cvalentine/Desktop/help-me.svg")))
    igv.close()
  }
}
