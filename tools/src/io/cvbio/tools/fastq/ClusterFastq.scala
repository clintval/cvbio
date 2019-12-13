package io.cvbio.tools.fastq

import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.commons.util.Logger
import com.fulcrumgenomics.fastq.{FastqSource, QualityEncoding, QualityEncodingDetector}
import com.fulcrumgenomics.sopt._
import io.cvbio.commons.CommonsDef._
import io.cvbio.commons.util.CsvWriter
import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}

@clp(
  description =
    """
      |Cluster and visualize the qualities within FASTQ reads.
    """,
  group = ClpGroups.FastaOrFastq
) class ClusterFastq(
  @arg(flag = 'i', doc = "The input FASTQ.") val input: PathToFastq
) extends CvBioTool {

  /** Run the tool [[ClusterFastq]]. */
  override def execute: Unit = {
    val encoding = ClusterFastq.detectQualityEncoding(Seq(input), logger = Some(logger))
    val writer   = CsvWriter(PathUtil.replaceExtension(input, CsvExtension))

    FastqSource(input)
      .map(_.quals.map(encoding.toStandardNumeric))
      .foreach(writer.write)

    writer.close()
  }
}

/** Companion object to [[ClusterFastq]]. */
object ClusterFastq {

  /** Detect the quality encoding in a collection of FASTQ files. */
  def detectQualityEncoding(input: Seq[PathToFastq], logger: Option[Logger] = None): QualityEncoding = {
    val detector  = new QualityEncodingDetector
    detector.sample(input.iterator.flatMap(FastqSource.apply).map(_.quals))
    val encodings = detector.rankedCompatibleEncodings (q = 30)
    require(encodings.nonEmpty, "Could not determine quality score encoding in FASTQ. No known encodings are valid for all observed qualities.")
    if (encodings.size > 1) logger.foreach(_.warning(s"Making ambiguous determination about fastq's quality encoding; possible encodings: ${encodings.mkString (", ")}."))
    logger.foreach(_.info(s"Auto-detected quality format as: ${encodings.head}"))
    encodings.head
  }
}
