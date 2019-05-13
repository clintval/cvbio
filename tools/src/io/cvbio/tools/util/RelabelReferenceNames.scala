package io.cvbio.tools.util

import com.fulcrumgenomics.FgBioDef.FgBioEnum
import com.fulcrumgenomics.commons.util.DelimitedDataParser
import com.fulcrumgenomics.sopt._
import com.fulcrumgenomics.util.Io
import enumeratum.EnumEntry
import io.cvbio.commons.CommonsDef._
import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import io.cvbio.tools.util.RelabelReferenceNames._

import scala.collection.immutable

@clp(
  description =
    """
      |Relabel reference sequence names using defined chromosome mapping tables.
      |
      |The mapping tables that are packaged with this tool are maintained at the following URL:
      |
      | - https://github.com/dpryan79/ChromosomeMappings
    """,
  group = ClpGroups.Util
) class RelabelReferenceNames(
  @arg(flag = 'i', doc = "The input file.") val in: FilePath = Io.StdIn,
  @arg(flag = 'o', doc = "The output file.") val out: FilePath = Io.StdOut,
  @arg(flag = 'a', doc = "The input genome assembly.") val assembly: Option[GenomeAssembly] = None,
  @arg(flag = 'f', doc = "The input reference name set.") val inputFormat: Option[ReferenceNameFormat] = None,
  @arg(flag = 't', doc = "The output reference name set.") val outputFormat: Option[ReferenceNameFormat] = None,
  @arg(flag = 'c', doc = "The column names to convert, 1-indexed", minElements = 1) val columns: Seq[Int] = Seq(1),
  @arg(flag = 'm', doc = "A two-column tab-delimited mapping file.", mutex = Array("assembly", "inputFormat", "outputFormat")) val mappingFile: Option[FilePath] = None,
  @arg(flag = 'd', doc = "File delimiter") val delimiter: Char = '\t',
  @arg(flag = 'x', doc = "Drop records which do not have a mapping") val drop: Boolean = true,
  @arg(doc = "Require that all reference names in the input file exist in the mapping") val requireExists: Boolean = true
) extends CvBioTool {

  validate(
    Seq(inputFormat, outputFormat, assembly).map(_.isEmpty).distinct.length == 1,
    "All of `assembly`, `inputFormat`, and `outputFormat` must be defined, or none at all."
  )

  override def execute(): Unit = {
    Io.assertReadable(in)
    Io.assertCanWriteFile(out)
    mappingFile.foreach(Io.assertReadable)

    // https://github.com/TGAC/earlham-galaxytools/blob/master/tools/replace_chromosome_names/replace_chromosome_names.py
    val lines = (assembly, inputFormat, outputFormat, mappingFile) match {
      case (Some(as), Some(from), Some(to), None) => RelabelReferenceNames.nameMap(s"${as}_${from}2$to.txt")
      case (None, None, None, Some(file))         => RelabelReferenceNames.nameMap(file)
      case _                                      => unreachable("CLI validators should never let this happen!")
    }

    lines.foreach(println)
  }
}

object RelabelReferenceNames {

  /** The `resources/` directory name in the packaged JAR. */
  val ResourcesDirectoryName: String = "/chromosome-mappings"

  /** The Chromosome Mappings filename pattern. */
  private val ReferenceMappingFilenamePattern: String = "\\w.*_.*2.*\\.txt"

  private def nameMap(lines: Iterator[String]): Map[String, String] = {
    val rows = new DelimitedDataParser(lines, delimiter = '\t', header = Seq("1", "2"))
    rows.map { row => row[String](0) -> row[String](1) }.toMap
  }

  private def nameMap(filePath: FilePath): Map[String, String] = nameMap(Io.readLines(filePath))

  private def nameMap(filename: Filename): Map[String, String] = Map.empty // FIXME

  /** Trait that all enumeration values of type [[ReferenceNameFormat]] should extend. */
  sealed trait ReferenceNameFormat extends EnumEntry

  /** Contains enumerations of reference name formats. */
  object ReferenceNameFormat extends FgBioEnum[ReferenceNameFormat] {

    def values: immutable.IndexedSeq[ReferenceNameFormat] = findValues

    /** The value when [[ReferenceNameFormat]] is the Ensembl format. */
    case object Ensembl extends ReferenceNameFormat { override def toString = "ensembl"}

    /** The value when [[ReferenceNameFormat]] is the Gencode format. */
    case object Gencode extends ReferenceNameFormat { override def toString = "gencode"}

    /** The value when [[ReferenceNameFormat]] is the NCBI format. */
    case object NCBI extends ReferenceNameFormat

    /** The value when [[ReferenceNameFormat]] is the UCSC format. */
    case object UCSC extends ReferenceNameFormat

    /** The value when [[ReferenceNameFormat]] is the xenbase format. */
    case object Xenbase extends ReferenceNameFormat { override def toString = "xenbase"}
  }

  /** Trait that all enumeration values of type [[GenomeAssembly]] should extend. */
  sealed trait GenomeAssembly extends EnumEntry

  /** Contains enumerations of chromosome mapping genome assemblies. */
  object GenomeAssembly extends FgBioEnum[GenomeAssembly] {

    def values: scala.collection.immutable.IndexedSeq[GenomeAssembly] = findValues

    /** The value when [[GenomeAssembly]] is the assembly BDGP6. */
    case object BDGP6 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly dm version 3. */
    case object dm3 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly galGal version 4. */
    case object galGal4 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly GRCh version 37. */
    case object GRCh37 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly GRCh version 38. */
    case object GRCh38 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly GRCm version 37. */
    case object GRCm37 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly GRCm version 38. */
    case object GRCm38 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly GRCz version 10. */
    case object GRCz10 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly GRCz version 11. */
    case object GRCz11 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly JGI version 4.2. */
    case object JGI_4_2 extends GenomeAssembly { override def toString = "JGI_4.2"}

    /** The value when [[GenomeAssembly]] is the assembly MEDAKA1. */
    case object MEDAKA1 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly R64 version 1-1. */
    case object R64_1_1 extends GenomeAssembly { override def toString = "R64-1-1"}

    /** The value when [[GenomeAssembly]] is the assembly rn version 5. */
    case object rn5 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly Rnor version 6.0. */
    case object Rnor_6_0 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly WBcel235 version 235. */
    case object WBcel235 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly Xenopus_laevis version v2. */
    case object Xenopus_laevis_v2 extends GenomeAssembly

    /** The value when [[GenomeAssembly]] is the assembly Xenopus_tropicalis version v9.1. */
    case object Xenopus_tropicalis_v9_1 extends GenomeAssembly { override def toString = "Xenopus_tropicalis_v9.1" }

    /** The value when [[GenomeAssembly]] is the assembly Zv version v9. */
    case object Zv9 extends GenomeAssembly
  }
}
