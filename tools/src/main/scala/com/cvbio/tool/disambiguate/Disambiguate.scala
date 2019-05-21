package com.cvbio.tool.disambiguate


import com.cvbio.commons.CommonsDef._
import com.cvbio.tool.cmdline.{ClpGroups, CvBioTool}
import com.fulcrumgenomics.bam.api.SamSource
import com.fulcrumgenomics.commons.io.Io
import com.fulcrumgenomics.sopt._

@clp(
  description =
    """
      |Disambiguate reads that were mapped to two genomes.
      |
      |Disambiguation of dually mapped reads is performed per-template and all information across primary, secondary,
      |and supplementary alignments is used as evidence. Alignment disambiguation is commonly required when analyzing
      |sequencing data from transduction, transfection, xenographic (including patient derived xenografts), and
      |transgenic animal experiments. This tool works by comparing various alignment scores between a template that has
      |been mapped to two genomes in order to determine which genome is the most likely source.
      |
      |All templates which are positively assigned to a source genome are written to a genome-specific output BAM
      |file. Any templates with ambiguous genome assignment are written to a third output BAM.
      |
      |### Prior Art
      |
      |  - [Disambiguate](https://github.com/AstraZeneca-NGS/disambiguate) from AstraZeneca's NGS team
      |
      |### Glossary
      |
      |  - MAPQ: A metric that tells you how confident you can be that a read comes from a reported mapping position.
      |  - AS:   A metric that tells you how similar the read is to the reference sequence.
      |
      |### Notes
      |
      |  - Both input BAM files must be queryname sorted.
      |  - Only BAMs produced from the Burrows-Wheeler Aligner (bwa) are accepted at this time.
      |
    """,
  group  = ClpGroups.AdHoc
) class Disambiguate(
  @arg(flag = '1', doc = "The first BAM.") val bam1: PathToBam,
  @arg(flag = '2', doc = "The second BAM.") val bam2: PathToBam,
  @arg(flag = 'a', doc = "The aligner that was used to map both BAM files.") val aligner: Aligner = Aligner.Bwa,
  @arg(flag = 'p', doc = "The output file prefix (e.g. dir/sample_name).") val prefix: PathPrefix
) extends CvBioTool {

  //validate(bam1 != bam2, "BAM files cannot be the same.")
  validate(aligner == Aligner.Bwa, "Only `bwa` is supported at this time.")

  override def execute(): Unit = {
    Io.assertReadable(Seq(bam1, bam2))
    Io.assertCanWriteFile(prefix)

    val source1 = SamSource(bam1)
    val source2 = SamSource(bam2)

    AlignmentPair(source1, source2).foreach(println)
  }
}
