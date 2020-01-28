package io.cvbio.tools.vcf

import com.fulcrumgenomics.sopt._
\import io.cvbio.commons.CommonsDef._
import io.cvbio.commons.effectful.{Io, Rtg}
import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}

import scala.util.{Failure, Success}

@clp(
  description =
    """
      |Diff a VCF the cool way.
    """,
  group = ClpGroups.Util
) class VcfDiff(
  @arg(flag = '1', doc = "The reference VCF input file.") val vcf1: PathToVcf,
  @arg(flag = '2', doc = "The reference VCF input file.") val vcf2: PathToVcf,
  @arg(flag = 'r', doc = "An RTG Tools reference SDF file.") val ref: Rtg.PathToSdf,
  // @arg(flag = 'c', doc = "A `VcfDiff` configuration file.") val config: Option[FilePath] = None,
) extends CvBioTool {

  require(Rtg.available, "RTG Tools executable `rtg` must be available on the system path.")

  /** Run the tool [[VcfDiff]]. */
  override def execute(): Unit = {
    val rtgOutput = Io.makeTempDir(getClass.getSimpleName)

    Rtg.vcfEval(vcf1, vcf2, ref, output = rtgOutput) match { // The method `execCommand` underlying needs work.
      case Success(_) =>
      case Failure(e: Throwable) => throw e
    }
  }
}
