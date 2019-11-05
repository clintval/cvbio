package com.cvbio.pipeline.pipelines

import com.cvbio.commons.CommonsDef._
import com.cvbio.pipeline.cmdline.ClpGroups
import com.cvbio.pipeline.tasks.misc.MoveBam
import com.cvbio.pipeline.tasks.star.StarAlign
import com.cvbio.pipeline.tasks.star.StarAlign.{IntronMotifFilter, IntronStrandFilter, OutputFilterType, TwoPassMode}
import com.fulcrumgenomics.bam.api.SamSource
import com.fulcrumgenomics.commons.io.{Io, PathUtil}
import com.fulcrumgenomics.sopt.{arg, clp}
import dagr.core.execsystem.Cores
import dagr.core.tasksystem.Pipeline
import dagr.tasks.misc.{DeleteFiles, MakeDirectory}
import dagr.tasks.picard.{BuildBamIndex, MergeBamAlignment, SamToFastq, ValidateSamFile}

@clp(
  description =
    """
      |Align paired-end reads with the `STAR` aligner.
    """,
  group = ClpGroups.SamOrBam
) class StarAlignPipeline(
  @arg(flag = 'i', doc = "Path to the input unmapped BAM.") val input: PathToBam,
  @arg(flag = 'g', doc = "The STAR genome directory.") val genomeDir: DirPath,
  @arg(flag = 'p', doc = "The output prefix (e.g. /path/to/sample1)") val prefix: PathPrefix,
  @arg(flag = 'r', doc = "The reference genome.") val ref: Option[PathToFasta] = None,
  @arg(flag = 'M', doc = "The two-pass mode to use.") val twoPass: Option[TwoPassMode] = None,
  // Output Filter Options
  @arg(doc = "The output filter type.") val outputFilterType: OutputFilterType = OutputFilterType.Normal,
  @arg(doc = "The maximum mismatches per read pair.") val maxMismatchesPerPair: Option[Int] = None,
  @arg(doc = "The maximum mismatches per read length ratio.") val maxMismatchesPerReadLength: Option[Double] = None,
  @arg(doc = "The intron motif filter.") val intronMotifFilter: Option[IntronMotifFilter] = None,
  @arg(doc = "The intron strand filter.") val intronStrandFilter: Option[IntronStrandFilter] = Some(IntronStrandFilter.RemoveInconsistentStrands),
  // Align Options
  @arg(doc = "The minimum intron length.") val minimumIntronLength: Option[Int] = None,
  @arg(doc = "The maximum intron length.") val maximumIntronLength: Option[Int] = None,
  @arg(doc = "The maximum mate span.") val maximumMateSpan: Option[Int] = None,
  // Runtime resources
  @arg(doc = "The number of cores to use.") val cores: Cores = StarAlign.DefaultCores
) extends Pipeline(outputDirectory = Some(prefix.getParent)) {

  override def build(): Unit = {
    Io.assertReadable(Seq(input) ++ ref)
    Io.assertCanWriteFile(prefix)
    Io.assertListable(genomeDir)

    val outputPrefix = if (prefix.endsWith(".")) prefix else PathUtil.pathTo(prefix + ".")

    val source         = SamSource(input)
    val firstReadGroup = yieldAndThen(source.header.getReadGroups.toStream.headOption)(source.safelyClose())
    val id             = firstReadGroup.flatMap(rg => Option(rg.getId))
    val sampleName     = firstReadGroup.flatMap(rg => Option(rg.getSample))
    val library        = firstReadGroup.flatMap(rg => Option(rg.getLibrary))
    val platform       = firstReadGroup.flatMap(rg => Option(rg.getPlatform))
    val platformUnit   = firstReadGroup.flatMap(rg => Option(rg.getPlatformUnit))

    def temp(suffix: FilenameSuffix): FilePath = Io.makeTempFile(prefix = getClass.getSimpleName, suffix = suffix)

    val starBam: PathToBam = PathUtil.pathTo(outputPrefix + StarAlign.AlignedCoordinateSortedSuffix)
    val read1: PathToFastq = temp(".r1" + FqExtension)
    val read2: PathToFastq = temp(".r2" + FqExtension)
    val tempBam: PathToBam = temp(BamExtension)

    val prepare   = new MakeDirectory(outputPrefix.getParent)
    val makeFastq = new SamToFastq(in = input, fastq1 = read1, fastq2 = Some(read2), interleave = false)
    val align     = new StarAlign(
      read1                      = read1,
      read2                      = Some(read2),
      genomeDir                  = genomeDir,
      prefix                     = Some(outputPrefix),
      id                         = id,
      sampleName                 = sampleName,
      library                    = library,
      platform                   = platform,
      platformUnit               = platformUnit,
      twoPass                    = twoPass,
      outputFilterType           = outputFilterType,
      maxMismatchesPerPair       = maxMismatchesPerPair,
      maxMismatchesPerReadLength = maxMismatchesPerReadLength,
      intronMotifFilter          = intronMotifFilter,
      intronStrandFilter         = intronStrandFilter,
      minimumIntronLength        = minimumIntronLength,
      maximumIntronLength        = maximumIntronLength,
      maximumMateSpan            = maximumMateSpan,
      cores                      = cores
    )

    val postProcess = ref match {
      case None       => new BuildBamIndex(starBam)
      case Some(_ref) =>
        val merge        = new MergeBamAlignment(unmapped = input, mapped = starBam, out = tempBam, ref = _ref)
        val overwriteBam = new MoveBam(tempBam, starBam)
        val validate     = new ValidateSamFile(in = starBam, prefix = None, ref = _ref)
        merge ==> overwriteBam ==> validate
    }

    root ==> prepare ==> makeFastq ==> align ==> (postProcess :: new DeleteFiles(read1, read2))
  }
}
