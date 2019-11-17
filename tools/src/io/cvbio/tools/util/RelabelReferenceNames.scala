package io.cvbio.tools.util

import java.nio.file.{FileSystems, FileVisitOption, Files, Paths}
import java.util.Collections
import java.util.jar.JarFile
import java.util.zip.ZipInputStream

import io.cvbio.tools.util.RelabelReferenceNames.ResourcesDirectoryName
import com.fulcrumgenomics.commons.util.DelimitedDataParser
import com.fulcrumgenomics.sopt._
import com.fulcrumgenomics.util.Io
import io.cvbio.commons.CommonsDef._
import io.cvbio.commons.reflect.ReflectionUtil
import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}

import scala.collection.mutable.ListBuffer

@clp(
  description =
    """
      |Relabel reference sequence names using defined chromosome mapping tables.
      |
      |The mapping tables that are packaged with this tool are maintained at the following URL:
      |
      | - https://github.com/dpryan79/ChromosomeMappings
      |
      |### Prior Art
      |
      | - https://github.com/TGAC/earlham-galaxytools/blob/master/tools/replace_chromosome_names/replace_chromosome_names.py
    """,
  group = ClpGroups.Util
) class RelabelReferenceNames(
  @arg(flag = 'i', doc = "The input file.") val in: FilePath = Io.StdIn,
  @arg(flag = 'o', doc = "The output file.") val out: FilePath = Io.StdOut,
  @arg(flag = 'c', doc = "The column names to convert, 1-indexed", minElements = 1) val columns: Seq[Int] = Seq(1),
  @arg(flag = 'm', doc = "A two-column tab-delimited mapping file.") val mappingFile: Option[FilePath] = None,
  @arg(flag = 'd', doc = "File delimiter") val delimiter: Char = '\t',
  @arg(flag = 'x', doc = "Drop records which do not have a mapping") val drop: Boolean = true,
  @arg(doc = "Require that all reference names in the input file exist in the mapping") val requireExists: Boolean = true
) extends CvBioTool {

  override def execute(): Unit = {
    ???

  }
}

object RelabelReferenceNames {

  /** The `resources/` directory name in the packaged JAR. */
  val ResourcesDirectoryName: String = "/chromosome-mappings"

  /** The Chromosome Mappings filename pattern. */
  private val ReferenceMappingFilenamePattern: String = "\\w.*_.*2.*\\.txt"

  private def build(lines: Iterator[String]): Map[String, String] = {
    val rows = new DelimitedDataParser(lines, delimiter = '\t', header = Seq("1", "2"))
    rows.map { row => row[String](0) -> row[String](1) }.toMap
  }

  val something = ReflectionUtil.resourceListing(getClass)
}
