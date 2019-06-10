package com.cvbio.tool.bam

import com.cvbio.commons.CommonsDef._
import com.cvbio.tool.cmdline.{ClpGroups, CvBioTool}
import com.fulcrumgenomics.commons.io.Io
import com.fulcrumgenomics.sopt._

@clp(
  description =
    """
      |Lift alignments that align to more than one genomic span.
      |
      |For templates which align to more than one genomic span in the human genome, such when aligning a library to
      |regions of segmental duplication, lift them to only one of the genomic spans and fix their mapping quality. In
      |order for a template to be considered for lifting it must satisfy the following criteria:
      |
      |  - All template alignments must be wholly contained within all genomic spans.
      |  - A template must be ambiguously aligned to all genomic spans.
      |
      |The input interval lists describe the genomic spans to consider for alignment lifting. There must be an equal
      |number of intervals within all interval lists as they are considered paired. Template that match the above
      |criteria will be lifted from the `from` intervals, to the `to` intervals.
    """,
  group  = ClpGroups.SamOrBam
) class LiftAlignments(
  @arg(flag = 'i', doc = "The input BAM.") val in: PathToBam,
  @arg(flag = 'o', doc = "The output BAM.)") val out: PathToBam,
  @arg(flag = 'f', doc = "Lift alignments *from* these intervals.") val from: PathToIntervals,
  @arg(flag = 't', doc = "Lift alignments *to* these intervals.") val to: PathToIntervals
) extends CvBioTool {

  override def execute(): Unit = {
    Io.assertReadable(in)
    Io.assertCanWriteFile(out)
  }
}
