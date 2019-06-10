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
import dagr.tasks.picard.{BuildBamIndex, MergeBamAlignment, SamToFastq, ValidateSamFile}

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
  @arg(flag = 'i', doc = "Path to the input unmapped BAM.") val input: PathToBam,
  @arg(flag = 'g', doc = "The STAR genome directory.") val genomeDir: DirPath,
  @arg(flag = 'p', doc = "The output prefix (e.g. /path/to/sample1.)") val prefix: PathPrefix,
  @arg(flag = 'r', doc = "The reference genome.") val ref: Option[PathToFasta] = None,
  @arg(flag = 'a', doc = "The read group ID", mutex = Array("ref")) val id: Option[String] = None,
  @arg(flag = 's', doc = "The sample name", mutex = Array("ref")) val sampleName: Option[String] = None,
  @arg(flag = 'l', doc = "The library ID", mutex = Array("ref")) val library: Option[String] = None,
  @arg(flag = 'm', doc = "The platform (e.g. illumina).", mutex = Array("ref")) val platform: Option[String] = None,
  @arg(flag = 'u', doc = "The platform unit (e.g. run barcode)", mutex = Array("ref")) val platformUnit: Option[String] = None,
  @arg(flag = '2', doc = "The two-pass mode to use.") val twoPass: Option[TwoPassMode] = None,
  @arg(doc = "The number of cores to use.") val cores: Cores = StarAlign.DefaultCores
) extends Pipeline(outputDirectory = Some(prefix.getParent)) {

  override def build(): Unit = {
    Io.assertReadable(Seq(input) ++ ref)
    Io.assertCanWriteFile(prefix)
    Io.assertListable(genomeDir)

    def temp(suffix: FilenameSuffix): FilePath = Io.makeTempFile(prefix = getClass.getSimpleName, suffix = suffix)

    val starBam: PathToBam = PathUtil.pathTo(prefix.toString + StarAlign.AlignedCoordinateSortedSuffix)
    val read1: PathToFastq = temp("r1" + FqExtension)
    val read2: PathToFastq = temp("r2" + FqExtension)
    val tempBam: PathToBam = temp(BamExtension)

    val prepare   = new MakeDirectory(prefix.getParent)
    val makeFastq = new SamToFastq(in = input, fastq1 = read1, fastq2 = Some(read2), interleave = false)
    val align     = new StarAlign(
      read1        = read1,
      read2        = Some(read2),
      genomeDir    = genomeDir,
      prefix       = Some(prefix),
      id           = id,
      sampleName   = sampleName,
      library      = library,
      platform     = platform,
      platformUnit = platformUnit,
      twoPass      = twoPass,
      cores        = cores
    )

    val postProcess = ref match {
      case None       => new BuildBamIndex(starBam)
      case Some(_ref) =>
        def bai(bam: PathToBam): PathToBai = PathUtil.replaceExtension(bam, BaiExtension)

        val merge       = new MergeBamAlignment(unmapped = input, mapped = starBam, out = tempBam, ref = _ref)
        val deleteInput = new DeleteFiles(starBam)
        val moveBam     = new MoveFile(tempBam, starBam) ==> new MoveFile(bai(tempBam), bai(starBam))
        val validate    = new ValidateSamFile(in = starBam, prefix = None, ref = _ref)
        merge ==> deleteInput ==> moveBam ==> validate
    }

    root ==> align ==> new DeleteFiles(read1, read2)
    root ==> prepare ==> makeFastq ==> align ==> postProcess
  }
}
