package io.cvbio.tools.util

import com.fulcrumgenomics.commons.util.DelimitedDataParser
import com.fulcrumgenomics.sopt._
import com.fulcrumgenomics.util.Io
import io.cvbio.commons.CommonsDef._
import io.cvbio.commons.reflect.ReflectionUtil
import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}

@clp(
  description =
    """
      |Relabel reference sequence names using defined chromosome mapping tables.
      |
      |### References and Prior Art
      |
      | - https://github.com/dpryan79/ChromosomeMappings
      | - https://github.com/TGAC/earlham-galaxytools/blob/master/tools/replace_chromosome_names/replace_chromosome_names.py
    """,
  group = ClpGroups.Util
) class RelabelReferenceNames(
  @arg(flag = 'i', doc = "The input file.") val in: FilePath = Io.StdIn,
  @arg(flag = 'o', doc = "The output file.") val out: FilePath = Io.StdOut,
  @arg(flag = 'c', doc = "The column names to convert, 1-indexed", minElements = 1) val columns: Seq[Int] = Seq(1),
  @arg(flag = 't', doc = "One of the bundled mapping tables.") val mappingTable: String,
  @arg(flag = 'm', doc = "A two-column tab-delimited mapping file.") val mappingFile: Option[FilePath] = None,
  @arg(flag = 'd', doc = "File delimiter") val delimiter: Char = '\t',
  @arg(flag = 'x', doc = "Drop records which do not have a mapping") val drop: Boolean = true,
  @arg(doc = "Require that all reference names in the input file exist in the mapping") val requireExists: Boolean = true
) extends CvBioTool {

  override def execute(): Unit = {
    ???
  }
}

/** Companion object for [[RelabelReferenceNames]]. */
object RelabelReferenceNames {

  /** The resources directory name in the packaged JAR for all bundled mapping tables. */
  val ResourcesDirectoryName: String = "chromosome-mappings"

  /** The Chromosome Mappings filename pattern. */
  private val ReferenceMappingFilenamePattern: String = ".*_.*2.*\\.txt"

  /** Build. */
  private def build(lines: Iterator[String]): Map[String, String] = {
    val rows = new DelimitedDataParser(lines, delimiter = '\t', header = Seq("1", "2"))
    rows.map { row => row[String](index = 0) -> row[String](index = 1) }.toMap
  }

  /** All bundled mapping tables. */
  val MappingTables: Seq[String] = {
    ReflectionUtil
      .resourceListing(getClass)
      .filter(_.matches(ReferenceMappingFilenamePattern))
      .map(_.stripPrefix(ResourcesDirectoryName))
      .map(_.stripSuffix(TextExtension))
  }
}
