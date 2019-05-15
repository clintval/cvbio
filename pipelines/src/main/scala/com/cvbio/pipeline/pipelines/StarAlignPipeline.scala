package com.cvbio.pipeline.pipelines

import com.cvbio.commons.CommonsDef._
import com.cvbio.pipeline.cmdline.ClpGroups
import com.cvbio.pipeline.tasks.star.StarAlign
import com.cvbio.pipeline.tasks.star.StarAlign.TwoPassMode
import com.fulcrumgenomics.commons.io.{Io, PathUtil}
import com.fulcrumgenomics.sopt.{arg, clp}
import dagr.core.execsystem.Cores
import dagr.core.tasksystem.Pipeline
import dagr.tasks.DagrDef.PathToBai
import dagr.tasks.misc.{DeleteFiles, MakeDirectory, MoveFile}
import dagr.tasks.picard.{MergeBamAlignment, SamToFastq, ValidateSamFile}

@clp(
  description =
    """
      |Align paired-end reads with the `STAR` aligner.
      |
      |It is highly recommended that the original reference FASTA be known at runtime so the BAM can be processed by
      |Picard's [[MergeBamAlignment]]. If a reference is not provided then all additional read group metadata must be
      |supplied so the final BAM passes validation.
      |
      | - <prefix>Aligned.sortedByCoord.out.bam
      |
      |Note: At the moment, all read group information must be supplied, unless Picard's [[MergeBamAlignment]] is run.
      |      A future feature of this pipeline will be to look that information up on-the-fly from the input BAM.
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
  @arg(flag = '2', doc = "The two-pass mode to use.") val twoPass: Option[TwoPassMode] = None,
  @arg(doc = "The number of cores to use.") val cores: Cores = StarAlign.DefaultCores
) extends Pipeline(outputDirectory = Some(prefix.getParent)) {

  override def build(): Unit = {
    Io.assertReadable(Seq(input) ++ ref)
    Io.assertCanWriteFile(prefix)

    def bai(bam: PathToBam): PathToBai                          = PathUtil.replaceExtension(bam, BaiExtension)
    def f(prefix: PathPrefix, suffix: FilenameSuffix): FilePath = PathUtil.pathTo(prefix.toString + suffix)

    val read1: PathToFastq = f(prefix, "r1.fq")
    val read2: PathToFastq = f(prefix, "r2.fq")
    val starBam: PathToBam = f(prefix, StarAlign.AlignedCoordinateSortedSuffix)
    val tmpBam: PathToBam  = Io.makeTempFile(prefix = "tmp", suffix = BamExtension)

    val prepare   = new MakeDirectory(prefix.getParent)
    val makeFastq = new SamToFastq(in = input, fastq1 = read1, fastq2 = Some(read2), interleave = false)
    val align     = new StarAlign(
      read1        = read1,
      read2        = Some(read2),
      genomeDir    = genomeDir,
      prefix       = Some(prefix),
      sampleName   = sampleName,
      library      = library,
      platform     = platform,
      platformUnit = platformUnit,
      twoPass      = twoPass,
      cores        = cores
    )

    val maybeMergeAlignment = ref.map { _ref =>
      val merge       = new MergeBamAlignment(unmapped = input, mapped = starBam, out = tmpBam, ref = _ref)
      val deleteInput = new DeleteFiles(starBam)
      val moveBam     = new MoveFile(tmpBam, starBam) ==> new MoveFile(bai(tmpBam), bai(starBam))
      val validate    = new ValidateSamFile(in = starBam, prefix = None, ref = _ref)
      merge ==> deleteInput ==> moveBam ==> validate
    }

    root ==> align ==> new DeleteFiles(read1, read2)
    root ==> prepare ==> makeFastq ==> align ==> maybeMergeAlignment
  }
}
