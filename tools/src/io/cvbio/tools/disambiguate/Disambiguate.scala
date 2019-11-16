package io.cvbio.tools.disambiguate

import io.cvbio.bam.Bams.templatesIterator
import io.cvbio.commons.CommonsDef._
import io.cvbio.commons.effectful.Io
import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import io.cvbio.tools.disambiguate.Disambiguate.{AmbiguousOutputDirName, firstAssemblyName}
import io.cvbio.tools.disambiguate.DisambiguationStrategy.Classic
import com.fulcrumgenomics.bam.api.{SamOrder, SamSource, SamWriter}
import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.sopt.{arg, clp}

@clp(
  description =
    """
      |Disambiguate reads that were mapped to multiple references.
      |
      |Disambiguation of aligned reads is performed per-template and all information across primary, secondary, and
      |supplementary alignments is used as evidence. Alignment disambiguation is commonly used when analyzing sequencing
      |data from transduction, transfection, transgenic, or xenographic (including patient derived xenograft)
      |experiments. This tool works by comparing various alignment scores between a template that has been aligned to
      |many references in order to determine which reference is the most likely source.
      |
      |All templates which are positively assigned to a single source reference are written to a reference-specific
      |output BAM file. Any templates with ambiguous reference assignment are written to an ambiguous input-specific
      |output BAM file. Only BAMs produced from the Burrows-Wheeler Aligner (bwa) or STAR are currently supported.
      |
      |Input BAMs of arbitrary sort order are accepted, however, an internal sort to queryname will be performed unless
      |the BAM is already in queryname sort order. All output BAM files will be written in the same sort order as the
      |input BAM files. Although paired-end reads will give the most discriminatory power for disambiguation of short-
      |read sequencing data, this tool accepts paired, single-end (fragment), and mixed pairing input data.
      |
      |### Example
      |
      |To disambiguate templates that are aligned to human (A) and mouse (B):
      |
      |```
      |❯ java -jar cvbio.jar Disambiguate -i sample.A.bam sample.B.bam -p sample/sample -n hg38 mm10
      |
      |❯ tree sample/
      |  sample/
      |  ├── ambiguous-alignments/
      |  │  ├── sample.A.ambiguous.bai
      |  │  ├── sample.A.ambiguous.bam
      |  │  ├── sample.B.ambiguous.bai
      |  │  └── sample.B.ambiguous.bam
      |  ├── sample.hg38.bai
      |  ├── sample.hg38.bam
      |  ├── sample.mm10.bai
      |  └── sample.mm10.bam
      |```
      |
      |### Glossary
      |
      |  - MAPQ: A metric that tells you how confident you can be that a read comes from a reported mapping position.
      |  - AS:   A metric that tells you how similar the read is to the reference sequence.
      |  - NM:   A metric that measures the number of mismatches to the reference sequence (Hamming distance).
      |
      |### Prior Art
      |
      |  - [Disambiguate](https://github.com/AstraZeneca-NGS/disambiguate) from AstraZeneca's NGS team
    """,
  group  = ClpGroups.SamOrBam
) class Disambiguate(
  @arg(flag = 'i', doc = "The BAMs to disambiguate.") val input: Seq[PathToBam],
  @arg(flag = 'p', doc = "The output file prefix (e.g. dir/sample_name).") val prefix: PathPrefix,
  @arg(flag = 's', doc = "The disambiguation strategy to use.") private val strategy: DisambiguationStrategy = Classic,
  @arg(flag = 'n', doc = "The reference names. Default to the first Assembly Name in the BAM header.", minElements = 0) val referenceNames: Seq[String] = Seq.empty,
) extends CvBioTool {

  Io.assertReadable(input)

  /** Execute [[Disambiguate]] on all inputs. */
  override def execute(): Unit = {
    Seq(prefix.getParent, prefix.resolveSibling(AmbiguousOutputDirName)).foreach(Io.mkdirs)

    val sources            = input.map(bam => SamSource(bam))
    val ambiguousWriters   = input.map(bam => ambiguousWriter(bam, prefix = prefix))
    val unambiguousWriters = input.zip(finalizedNames).map { case (bam, name) => unambiguousWriter(bam, prefix = prefix, name = name) }

    templatesIterator(sources: _*)
      .foreach { templates =>
        strategy.choose(templates).map(templates.indexOf) match {
          case Some(index) => unambiguousWriters(index).write(templates(index).allReads)
          case None => templates.zip(ambiguousWriters).foreach { case (template, writer) => writer.write(template.allReads) }
        }
      }

    (ambiguousWriters ++ unambiguousWriters).foreach(_.close())
  }

  /** Return the finalized reference names to use when writing out disambiguated BAMs. */
  private[disambiguate] def finalizedNames: Seq[String] = {
    val names = if (referenceNames.nonEmpty) referenceNames else input.flatMap(firstAssemblyName)
    require(names.length == input.length, s"Not all BAM have a reference name defined. Found: ${names.mkString(", ")}")
    require(names.distinct.length == names.length, s"No redundant reference names allowed. Found: ${names.mkString(", ")}")
    names
  }

  /** Return an ambiguous SAM Writer that will write to a path within <prefix> making use of the <input> filename. */
  private[disambiguate] def ambiguousWriter(input: PathToBam, prefix: PathPrefix): SamWriter = {
    val source   = SamSource(input)
    val header   = source.header.clone()
    val filename = PathUtil.replaceExtension(input, s".ambiguous$BamExtension").getFileName
    val path     = prefix.resolveSibling(AmbiguousOutputDirName).resolve(filename)
    yieldAndThen(SamWriter(path = path, header = header, sort = SamOrder(header)))(source.safelyClose())
  }

  /** Return an unambiguous SAM Writer to a path starting with <prefix> and containing <name> infix. */
  private[disambiguate] def unambiguousWriter(input: PathToBam, prefix: PathPrefix, name: String): SamWriter = {
    val source = SamSource(input)
    val header = source.header.clone()
    val path   = PathUtil.pathTo(prefix + s".$name$BamExtension")
    yieldAndThen(SamWriter(path = path, header = header, sort = SamOrder(header)))(source.safelyClose())
  }
}

/** Companion object to [[Disambiguate]]. */
object Disambiguate {

  /** The directory in the output prefix that will hold ambiguous alignments. */
  val AmbiguousOutputDirName: Filename = "ambiguous-alignments"

  /** Return the first reference sequence assembly name from a SAM file's sequence dictionary. */
  private[disambiguate] def firstAssemblyName(input: PathToBam): Option[String] = {
    val source    = SamSource(input)
    val sequences = source.header.getSequenceDictionary.getSequences
    yieldAndThen(sequences.toStream.headOption.flatMap(record => Option(record.getAssembly)))(source.safelyClose)
  }
}
