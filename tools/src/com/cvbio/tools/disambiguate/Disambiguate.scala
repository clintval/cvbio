package com.cvbio.tools.disambiguate

import com.cvbio.bam.Bams.{TemplateUtil, templatesIterator}
import com.cvbio.commons.CommonsDef._
import com.cvbio.commons.io.Io.PathUtil
import com.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import com.cvbio.tools.disambiguate.Disambiguate.DisambiguationStrategy.ClassicDisambiguationStrategy
import com.cvbio.tools.disambiguate.Disambiguate.{DisambiguationStrategy, firstAssemblyName}
import com.fulcrumgenomics.FgBioDef.FgBioEnum
import com.fulcrumgenomics.bam.Template
import com.fulcrumgenomics.bam.api.{SamSource, SamWriter}
import com.fulcrumgenomics.commons.io.Io
import com.fulcrumgenomics.sopt._
import enumeratum.EnumEntry
import htsjdk.samtools.SAMTag.{AS, NM}

@clp(
  description =
    """
      |Disambiguate reads that were mapped to multiple references.
      |
      |Disambiguation of mapped reads is performed per-template and all information across primary, secondary, and
      |supplementary alignments is used as evidence. Alignment disambiguation is useful when analyzing sequencing data
      |from transduction, transfection, xenographic (including patient derived xenografts), and transgenic experiments.
      |This tool works by comparing various alignment scores between a template that has been mapped to many references
      |in order to determine which reference is the most likely source.
      |
      |All templates which are positively assigned to a single source reference are written to a reference-specific
      |output BAM file. Any templates with ambiguous reference assignment are currently dropped.
      |
      |### Caveats
      |
      |  - No ambiguous BAM is currently written to the output prefix.
      |  - All input BAMs must have an Assembly Name defined in the first sequence of the sequence dictionary.
      |  - All input BAM files must be queryname grouped and synchronized on the read name.
      |  - Only BAMs produced from the Burrows-Wheeler Aligner (bwa) and STAR are currently supported.
      |  - Only BAMs produced from the same aligner are currently supported.
      |
      |### Glossary
      |
      |  - MAPQ: A metric that tells you how confident you can be that a read comes from a reported mapping position.
      |  - AS:   A metric that tells you how similar the read is to the reference sequence.
      |  - NM:   A metric that measures the number of mismatches to th reference sequence (Hamming distance).
      |
      |### Features for a Future Release
      |
      |  - Override the assembly names (output BAM prefixes)
      |  - Support `tophat` or `hisat2` alignments.
      |  - Check whether mixed aligners have been used and raise exception.
      |
      |### Prior Art
      |
      |  - [Disambiguate](https://github.com/AstraZeneca-NGS/disambiguate) from AstraZeneca's NGS team
    """,
  group  = ClpGroups.SamOrBam
) class Disambiguate(
  @arg(flag = 'i', doc = "The queryname-sorted BAMs to disambiguate.") val input: Seq[PathToBam],
  @arg(flag = 'p', doc = "The output file prefix (e.g. dir/sample_name).") val prefix: PathPrefix,
  @arg(flag = 's', doc = "The disambiguation strategy to use.") val strategy: DisambiguationStrategy = ClassicDisambiguationStrategy
) extends CvBioTool {

  override def execute(): Unit = {
    Io.mkdirs(prefix.getParent)
    Io.assertCanWriteFile(prefix)

    val writers = sources
      .zip(assemblyNames)
      .map { case (source, name) => SamWriter(path = prefix + s".$name$BamExtension", header = source.header) }

    templatesIterator(sources: _*)
      .foreach { templates =>
        val index: Option[Int] = strategy.indexOfBest(templates)
        index.foreach(i => writers(i).write(templates(i).allReads))
      }

    writers.foreach(_.close())
  }

  /** Fetch the first assembly names from the input BAMs. */
  private def assemblyNames: Seq[String] = {
    val names = sources.flatMap(source => firstAssemblyName(source))
    require(names.lengthCompare(sources.length) == 0, s"Not all BAM have their first assembly name defined. Only found: ${names.mkString(", ")}")
    require(names.distinct.lengthCompare(names.length) == 0, s"BAMs with the same assembly name are not allowed: ${names.mkString(", ")}")
    names
  }

  /** Check that each BAM is readable and return its [[SamSource]]. */
  private def sources: Seq[SamSource] = {
    Io.assertReadable(input)
    input.map(path => SamSource(path))
  }
}

/** Companion object to [[Disambiguate]]. */
object Disambiguate {

  /** Look up the first reference sequence assembly name from the SamSource's sequence dictionary. */
  private[disambiguate] def firstAssemblyName(source: SamSource): Option[String] = {
    source
      .header
      .getSequenceDictionary
      .getSequences
      .toStream
      .headOption
      .flatMap { record => Option(record.getAssembly) }
  }

  /** Trait that all enumeration values of type [[DisambiguationStrategy]] should extend. */
  sealed trait DisambiguationStrategy extends EnumEntry {

    /** Take in a sequence of templates and return the the one with the most optimal alignment. */
    def choose(templates: Seq[Template]): Option[Template]

    /** Return the index of the most optimally aligned template. */
    def indexOfBest(templates: Seq[Template]): Option[Int] = choose(templates).map(templates.indexOf)
  }

  /** Contains enumerations of template disambiguation strategies. */
  object DisambiguationStrategy extends FgBioEnum[DisambiguationStrategy] {

    /** Return all available disambiguation strategies. */
    def values: scala.collection.immutable.IndexedSeq[DisambiguationStrategy] = findValues

    /** Test if all reads of all templates are unmapped or not. */
    private def allUnmapped(templates: Seq[Template]): Boolean = {
      templates.map(_.allReads.forall(_.unmapped == true)).forall(_ == true)
    }

    /** The value when [[DisambiguationStrategy]] is the original published algorithm. */
    case object ClassicDisambiguationStrategy extends DisambiguationStrategy {

        /** Pick the template by using the following logic:
          *
          * 1. Choose the template with the highest max alignment score, if there are multiple, move on.
          * 2. Choose the template with the highest min alignment score, if there are multiple, move on.
          * 3. Choose the template with the lowest min alignment edit distance, if there are multiple, move on.
          * 4. Choose the template with the lowest max alignment edit distance, if there are multiple, move on.
          * 5. Return None because this template is ambiguous across all alignments.
          *
          * While making choices, ensure we always compare read1 against read1 in all templates, and read2 against read2.
          * */
      def choose(templates: Seq[Template]): Option[Template] = {

        /** Implicits for counting the number of maximum properties in a container of items. */
        implicit class WithMinMaxCount[T](self: Iterable[T]) {

          /** Return the number of maximally occuring items in a container of items. */
          def maxCount(fn: T => Int): Int = self.count(p => self.map(fn).reduceOption(_ max _).contains(fn(p)))
        }

        /** A container to hold the best alignment scores per [[Template]]. */
        case class BestAlignmentScores(r1AS: Int, r2AS: Int, r1NM: Int, r2NM: Int) {
          def maxAS: Int = r1AS max r2AS
          def minAS: Int = r1AS min r2AS
          def maxNM: Int = r1NM max r2NM
          def minNM: Int = r1NM min r2NM
        }

        /** Companion object to [[BestAlignmentScores]]. */
        object BestAlignmentScores {

          /** Build a [[BestAlignmentScores]] from a [[Template]]. */
          def apply(template: Template): BestAlignmentScores = {
            new BestAlignmentScores(
              r1AS = template.allR1[Int](AS).flatten.reduceOption(_ max _).getOrElse(0),
              r2AS = template.allR2[Int](AS).flatten.reduceOption(_ max _).getOrElse(0),
              r1NM = template.allR1[Int](NM).flatten.reduceOption(_ min _).getOrElse(Int.MaxValue),
              r2NM = template.allR2[Int](NM).flatten.reduceOption(_ min _).getOrElse(Int.MaxValue)
            )
          }
        }

        val bestScores = templates.map(BestAlignmentScores.apply)

        if (templates.isEmpty || allUnmapped(templates)) {
          None
        } else if (bestScores.maxCount(_.maxAS) == 1) {
          Some(templates.maxBy(BestAlignmentScores(_).maxAS))
        } else if (bestScores.maxCount(_.minAS) == 1) {
          Some(templates.maxBy(BestAlignmentScores(_).minAS))
        } else if (bestScores.maxCount(_.minNM) == 1) {
          Some(templates.minBy(BestAlignmentScores(_).minNM))
        } else if (bestScores.maxCount(_.maxNM) == 1) {
          Some(templates.minBy(BestAlignmentScores(_).maxNM))
        } else { None }
      }
    }
  }
}
