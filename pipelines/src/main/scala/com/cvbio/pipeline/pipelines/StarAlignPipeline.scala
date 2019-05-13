package com.cvbio.pipeline.pipelines

import com.cvbio.commons.CommonsDef._
import com.cvbio.pipeline.cmdline.ClpGroups
import com.cvbio.pipeline.tasks.star.StarAlign
import com.fulcrumgenomics.commons.io.{Io, PathUtil}
import com.fulcrumgenomics.sopt.{arg, clp}
import dagr.core.execsystem.Cores
import dagr.core.tasksystem.{Pipeline, Task}
import dagr.tasks.DagrDef.PathToBai
import dagr.tasks.misc.{DeleteFiles, MakeDirectory, MoveFile}
import dagr.tasks.picard.{AddOrReplaceReadGroups, MergeBamAlignment, SamToFastq, ValidateSamFile}

@clp(
  description =
    """
      |Align paired-end reads with the `STAR` aligner.
      |
      |It is highly recommended that the original reference FASTA be known at runtime so the BAM can be processed by
      |Picard's [[MergeBamAlignment]]. If a reference is not provided then all additional read group metadata must be
      |supplied so the final BAM passes validation.
      |
      | - <prefix>Aligned.sortedByCoord.out.merged.bam
      |
      |Note: All `STAR` outputs begin with the same file path prefix. For the sake of output file organization, it is
      |      recommended to terminate the file path prefix with a single period (".").
    """,
  group = ClpGroups.Rna
) class StarAlignPipeline(
  @arg(flag = 'i', doc = "Path to the input BAM.") val input: PathToBam,
  @arg(flag = 'g', doc = "The STAR genome directory.") val genomeDir: DirPath,
  @arg(flag = 'd', doc = "The output prefix (e.g. /path/to/sample1.)") val prefix: PathPrefix,
  @arg(flag = 'r', doc = "The reference genome.") val ref: Option[PathToFasta] = None,
  @arg(flag = 's', doc = "The sample name", mutex = Array("ref")) val sampleName: Option[String] = None,
  @arg(flag = 'l', doc = "The library ID", mutex = Array("ref")) val library: Option[String] = None,
  @arg(flag = 'p', doc = "The platform (e.g. illumina).", mutex = Array("ref")) val platform: Option[String] = None,
  @arg(flag = 'u', doc = "The platform unit (e.g. run barcode)", mutex = Array("ref")) val platformUnit: Option[String] = None,
  @arg(doc = "The number of cores to use.") val cores: Cores = StarAlign.DefaultCores
) extends Pipeline(outputDirectory = Some(prefix.getParent)) {

  if (ref.isEmpty) require(
    Seq(sampleName, library, platform, platformUnit).forall(_.nonEmpty),
    "If no reference FASTA is provided, all read group information must be supplied."
  )

  override def build(): Unit = {
    Io.assertReadable(input)
    ref.foreach(Io.assertReadable)
    Io.assertCanWriteFile(prefix)

    def bai(bam: PathToBam): PathToBai = PathUtil.replaceExtension(bam, ".bai")

    val read1: PathToFastq = PathUtil.pathTo(prefix.toString + "raw.r1.fq")
    val read2: PathToFastq = PathUtil.pathTo(prefix.toString + "raw.r2.fq")
    val starBam: PathToBam = PathUtil.pathTo(prefix.toString + StarAlign.AlignedCoordinateSortedSuffix)
    val tmpBam: PathToBam  = PathUtil.replaceExtension(starBam, ".tmp.bam")

    val prepare   = new MakeDirectory(prefix.getParent)
    val makeFastq = new SamToFastq(in = input, fastq1 = read1, fastq2 = Some(read2), interleave = false)
    val align     = new StarAlign(read1 = read1, read2 = Some(read2), genomeDir = genomeDir, prefix = Some(prefix), cores = cores)

    val post = (ref, sampleName, library, platform, platformUnit) match {
      case (Some(r), None, None, None, None) => new MergeBamAlignment(
        unmapped = input,
        mapped   = starBam,
        out      = tmpBam,
        ref      = r
      )
      case (None, Some(s), Some(l), Some(p), Some(u)) => new AddOrReplaceReadGroups(
        in           = starBam,
        out          = tmpBam,
        sampleName   = s,
        library      = l,
        platform     = p,
        platformUnit = u
      )
      case _ => unreachable("CLI validators should never let this happen!")
    }

    val overwrite = new DeleteFiles(starBam) ==> new MoveFile(tmpBam, starBam) ==> new MoveFile(bai(tmpBam), bai(starBam))
    val cleanup   = new DeleteFiles(read1, read2)

    val maybeValidate: Option[Task] = ref match {
      case None       => None
      case Some(_ref) => Some(new ValidateSamFile(in = starBam, prefix = None, ref = _ref))
    }

    root ==> align ==> cleanup
    root ==> prepare ==> makeFastq ==> align ==> post ==> overwrite ==> maybeValidate
  }
}
