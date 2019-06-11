package com.cvbio.tool.disambiguate


import com.cvbio.bam.Bams._
import com.cvbio.commons.CommonsDef._
import com.cvbio.tool.cmdline.{ClpGroups, CvBioTool}
import com.fulcrumgenomics.FgBioDef.FgBioEnum
import com.fulcrumgenomics.bam.Template
import com.fulcrumgenomics.bam.api.SamSource
import com.fulcrumgenomics.commons.io.Io
import com.fulcrumgenomics.sopt._
import enumeratum.EnumEntry

@clp(
  description =
    """
      |Disambiguate reads that were mapped to multiple genomes.
      |
      |Disambiguation of mapped reads is performed per-template and all information across primary, secondary, and
      |supplementary alignments is used as evidence. Alignment disambiguation is useful when analyzing sequencing data
      |from transduction, transfection, xenographic (including patient derived xenografts), and transgenic experiments.
      |This tool works by comparing various alignment scores between a template that has been mapped to many genomes in
      |order to determine which genome is the most likely source.
      |
      |All templates which are positively assigned to a single source genome are written to a genome-specific output BAM
      |file. Any templates with ambiguous genome assignment are written to an ambiguous output BAM.
      |
      |### Notes
      |
      |  - All input BAM files must be queryname grouped and synchronized on the read name.
      |  - Only BAMs produced from the Burrows-Wheeler Aligner (bwa) are supported.
      |
      |### Glossary
      |
      |  - MAPQ: A metric that tells you how confident you can be that a read comes from a reported mapping position.
      |  - AS:   A metric that tells you how similar the read is to the reference sequence.
      |  - NM:   A metric that measures the number of mismatches to th reference sequence (Hamming distance).
      |
      |### Prior Art
      |
      |  - [Disambiguate](https://github.com/AstraZeneca-NGS/disambiguate) from AstraZeneca's NGS team
    """,
  group  = ClpGroups.AdHoc
) class Disambiguate(
  @arg(flag = 'b', doc = "The BAM files to disambiguate.") val bam: Seq[PathToBam],
  @arg(flag = 'p', doc = "The output file prefix (e.g. dir/sample_name).") val prefix: PathPrefix,
  @arg(flag = 'n', doc = "The reference names used to align the input BAMs. Will be inferred, if not provided.", minElements = 0) val name: Seq[String] = Seq.empty,
  @arg(flag = 's', doc = "The disambiguation strategy to use.") val strategy: Disambiguate.Strategy = Disambiguate.Strategy.Classic
) extends CvBioTool {

  if (name.nonEmpty) validate(bam.length == name.length, "You must provide an equal number of names for as many input BAMs.")

  override def execute(): Unit = {
    Io.assertReadable(bam)
    Io.assertCanWriteFile(prefix)

    val sources   = bam.map(path => SamSource(path))
    val names     = if (name.nonEmpty) name else Disambiguate.firstAssemblyName(sources).map(as => as.getOrElse(throw new IllegalStateException(s"No assembly name defined: $as")))
    val templates = templatesIterator(sources: _*)

    templates.map(strategy.pickTemplate).foreach(ts => println(ts))
  }
}

/** Companion object to [[Disambiguate]]. */
object Disambiguate {

  /** Look up the reference sequence dictionary and pull the assembly name from the first reference sequence. Return
    * None if there are no sequences in the sequence dictionary or the first sequence does not have it's assembly name
    * defined.
    * */
  def firstAssemblyName(source: SamSource): Option[String] = {
    source
      .header
      .getSequenceDictionary
      .getSequences
      .toStream
      .headOption
      .map(_.getAssembly)
  }

  /** Look up the reference sequence dictionary and pull the assembly name from the first reference sequence. Return
    * None if there are no sequences in the sequence dictionary or the first sequence does not have it's assembly name
    * defined.
    * */
  def firstAssemblyName(sources: Seq[SamSource]): Seq[Option[String]] = {
    sources.map(source => Disambiguate.firstAssemblyName(source))
  }

  /** Trait that all enumeration values of type [[Strategy]] should extend. */
  sealed trait Strategy extends EnumEntry { def pickTemplate(templates: Seq[Template]): Option[Int] }

  /** Contains enumerations of disambiguation strategies. */
  object Strategy extends FgBioEnum[Strategy] {

    def values: scala.collection.immutable.IndexedSeq[Strategy] = findValues

    /** The value when [[Strategy]] is the original published algorithm. */
    case object Classic extends Strategy {

      /** Pick the template by using the following workflow:
        *
        * 1. Choose the template (primary or secondary alignment) with the highest max alignment score. Continue if the
        *     max alignment scores across template alignments are equal.
        * 2. Choose the template (primary or secondary alignment) with the highest min alignment score. Continue if the
        *     min alignment scores across template alignments are equal.
        * 3. Choose the template (primary or secondary alignment) with the lowest min `NM` tag (edit distance). Continue
        *     if the min edit distances across template alignments are equal.
        * 4. Choose the template (primary or secondary alignment) with the lowest max `NM` tag (edit distance). Continue
        *     if the max edit distances across template alignments are equal.
        * 5. Return None as this template is ambiguous across alignments.
        */
      def pickTemplate(templates: Seq[Template]): Option[Int] = None // TODO: Write out logic here.
    }
  }
}
