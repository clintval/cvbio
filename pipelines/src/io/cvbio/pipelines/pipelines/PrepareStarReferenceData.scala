package io.cvbio.pipelines.pipelines

import com.fulcrumgenomics.commons.io.Io
import com.fulcrumgenomics.sopt.{arg, clp}
import dagr.core.execsystem.Cores
import dagr.core.tasksystem.Pipeline
import io.cvbio.commons.CommonsDef.{DirPath, PathToFasta, PathToGtf}
import io.cvbio.pipelines.cmdline.ClpGroups
import io.cvbio.pipelines.tasks.star.StarGenerateGenome
import io.cvbio.pipelines.tasks.star.StarGenerateGenome.DefaultCores

@clp(
  description =
    """
      |Prepare reference data for the `STAR` aligner.
      |
      |This pipeline produces binary genome sequences, suffix arrays, text-based chromosome names/lengths, splice
      |junctions coordinates, and transcripts/gene information. `STAR` reference data must be generated before
      |invocation of the `STAR` aligner upon RNA-seq data. The splice junction overhang size specifies the length of
      |genomic sequence around the annotated junction to be used in constructing the splice junctions database. Ideally,
      |this value should be equal to:
      |
      |```
      |Read Length - 1
      |```
      |
      |Where "Read Length" is the length of the reads. For example, given Illumina 100 bp paired-end reads, the ideal
      |value is `100 - 1 == 99`. In the case of varying read length, the ideal value is:
      |
      |```
      |max(Read Length) - 1
      |```
      |
      |In most cases, the default value of 100 will work as well as the ideal value.
      |
      |For more information, reference the `STAR` manual:
      |
      |  - http://labshare.cshl.edu/shares/gingeraslab/www-data/dobin/STAR/STAR.posix/doc/STARmanual.pdf
    """,
  group = ClpGroups.Utility
) class PrepareStarReferenceData(
  @arg(flag = 'i', doc = "Input FASTA files.") val fasta: Seq[PathToFasta],
  @arg(flag = 'g', doc = "The transcripts in GTF format.") val gtf: PathToGtf,
  @arg(flag = 'o', doc = "The output directory.") val out: DirPath,
  @arg(doc = "The splice junction overhang size.") val overhang: Int = 100,
  @arg(doc = "The number of threads to use.") val threads: Cores = DefaultCores
) extends Pipeline {

  override def build(): Unit = {
    Io.assertReadable(fasta :+ gtf)
    Io.mkdirs(out)

    root ==> new StarGenerateGenome(fasta = fasta, gtf = gtf, outputDir = out, overhang = overhang, threads = threads)
  }
}
