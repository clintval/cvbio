package io.cvbio.tools.util

import com.fulcrumgenomics.commons.util.DelimitedDataParser
import com.fulcrumgenomics.sopt._
import com.fulcrumgenomics.util.Io
import io.cvbio.commons.CommonsDef._
import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import io.cvbio.tools.util.RelabelReferenceNames.{DefaultDelimiter, SkipLinePrefixes, relabel}

import scala.collection.mutable.ListBuffer
import scala.util.Properties.lineSeparator
import scala.util.{Failure, Success, Try}

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
  @arg(flag = 'm', doc = "A two-column tab-delimited mapping file.") val mappingFile: FilePath,
  @arg(flag = 'c', doc = "The column names to convert, 1-indexed.", minElements = 1) val columns: Seq[Int] = Seq(1),
  @arg(flag = 'd', doc = "The input file data delimiter.") val delimiter: Char = DefaultDelimiter,
  @arg(flag = 's', doc = "Directly write-out columns that start with this prefix.") val skipPrefix: Seq[String] = SkipLinePrefixes,
  @arg(flag = 'x', doc = "Drop records which do not have a mapping.") val drop: Boolean = true
) extends CvBioTool {

  /** Run the tool [[RelabelReferenceNames]]. */
  override def execute(): Unit = {
    val source = Io.toSource(in)
    val writer = Io.toWriter(out)
    val lookup = RelabelReferenceNames.buildMapping(mappingFile, delimiter = DefaultDelimiter)

    source.getLines.zipWithIndex.foreach { case (line: String, lineNumber: Int) =>
      if (skipPrefix.exists(line.startsWith)) {
        writer.write(line + lineSeparator)
      } else {
        Try(relabel(line, columns, delimiter, lookup)) match {
          case Success(patched) => writer.write(patched.mkString(delimiter.toString) + lineSeparator)
          case Failure(_: NoSuchElementException) if drop => {
            logger.info(s"Dropping record ${lineNumber + 1} as at least one field did not have a mapping: $line.")
          }
        }
      }
    }
  }
}

/** Companion object for [[RelabelReferenceNames]]. */
object RelabelReferenceNames {

  /** The default delimiter to use for most text files. */
  val DefaultDelimiter: Char = '\t'

  /** Skip lines with these prefixes. */
  val SkipLinePrefixes: Seq[String] = Seq("#")

  /** Build a reference sequence name mapping. */
  private[util] def buildMapping(lines: Iterator[String], delimiter: Char = DefaultDelimiter): Map[String, String] = {
    val rows = new DelimitedDataParser(lines, delimiter = delimiter, header = Seq("1", "2"))
    rows.map { row => row[String](index = 0) -> row[String](index = 1) }.toMap
  }

  /** Build a reference sequence name mapping. */
  private[util] def buildMapping(file: FilePath, delimiter: Char): Map[String, String] = {
    buildMapping(Io.toSource(file).getLines(), delimiter = delimiter)
  }

  /** Relabel a line of delimited data using the function <using> for each index in <at>. */
  private[util] def relabel(line: String, at: Seq[Int], delimiter: Char = DefaultDelimiter, using: String => String): String = {
    val patched = patchManyWith(line.split(delimiter), at, using)
    patched.mkString(delimiter.toString)
  }
}
