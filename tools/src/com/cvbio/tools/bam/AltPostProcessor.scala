package com.cvbio.tools.bam

import com.cvbio.commons.CommonsDef._
import com.cvbio.commons.MathUtil.Ratio
import com.cvbio.tools.cmdline.ClpGroups
import com.cvbio.tools.bam.AltPostProcessor.MinPaRatio
import com.cvbio.tools.cmdline.CvBioTool
import com.fulcrumgenomics.commons.io.Io
import com.fulcrumgenomics.sopt._
import eu.timepit.refined.auto._

@clp(
  description =
    """
      |Post-process a BAM that was created from alignments created in alt-aware mode.
      |
      |Note: ALT loci alignment records that can align to both the primary assembly and alternate contig(s) will have a
      |      `pa` tag on the primary assembly alignment. The `pa` tag measures how much better a read aligns to its best
      |      alternate contig alignment versus its primary assembly (pa) alignment. Specifically, it is the ratio of
      |      the primary assembly alignment score over the highest alternate contig alignment score.
      |
      |Note: This script extracts the XA tag, lifts the mapping positions of ALT hits to the primary assembly, groups
      |      them and then estimates mapQ across groups. If a non-ALT hit overlaps a lifted ALT hit, its mapping quality
      |      is set to the smaller between its original mapQ and the adjusted mapQ of the ALT hit. If multiple ALT hits
      |      are lifted to the same position, they will yield new SAM lines with the same mapQ.
      |
      |#### Prior Art
      |
      |  - [BWA Kit](https://github.com/lh3/bwa/blob/master/bwakit/bwa-postalt.js)
      |
      |#### Resources
      |
      |  - https://gatkforums.broadinstitute.org/gatk/discussion/8017/how-to-map-reads-to-a-reference-with-alternate-contigs-like-grch38
    """,
  group  = ClpGroups.SamOrBam
) class AltPostProcessor(
  @arg(flag = 'i', doc = "The input BAM.") val in: PathToBam,
  @arg(flag = 'p', doc = "The output prefix (e.g. /path/to/sample1.)") val prefix: PathToBam,
  @arg(flag = 'r', doc = "Reduce mapQ to 0 if not overlapping lifted best and `pa` is less than this number") val minPaRatio: Ratio = MinPaRatio
) extends CvBioTool {

  require(minPaRatio >= 0 && minPaRatio <= 1, s"The minimum `pa` ratio must be within 0 to 1 inclusive. Found: $minPaRatio")

  override def execute(): Unit = {
    Io.assertReadable(in)
    Io.assertCanWriteFile(prefix)
  }
}

/** Constants and defaults that are used across the [[AltPostProcessor]]. */
object AltPostProcessor {


  /** The minimum ratio of the primary assembly alignment score over the highest alternate contig alignment score. */
  val MinPaRatio: Ratio = 0.2

  /** The tag which stores the ratio of alignment scores between a primary assembly alignment and the highest alternate
    * contig alignment score.
    */
  val PaTag: SamTag = "pa"
}
