package io.cvbio.tools.util

import com.fulcrumgenomics.commons.util.DelimitedDataParser
import com.fulcrumgenomics.sopt._
import com.fulcrumgenomics.util.Io
import io.cvbio.commons.CommonsDef._
import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import io.cvbio.tools.util.RelabelReferenceNames.{DefaultDelimiter, SkipLinePrefixes}

import scala.util.Properties.lineSeparator

@clp(
  description =
    """
      |Relabel reference sequence names using defined chromosome mapping tables.
      |
      |A collection of mapping tables is maintained at the following location:
      |
      | - https://github.com/dpryan79/ChromosomeMappings
    """,
  group = ClpGroups.Util
) class RelabelReferenceNames(
  @arg(flag = 'i', doc = "The input file.") val in: FilePath,
  @arg(flag = 'o', doc = "The output file.") val out: FilePath,
  @arg(flag = 'm', doc = "A two-column tab-delimited mapping file.") val mappingFile: FilePath,
  @arg(flag = 'c', doc = "The column names to convert, 1-indexed.", minElements = 1) val columns: Seq[Int] = Seq(1),
  @arg(flag = 'd', doc = "The input file data delimiter.") val delimiter: Char = DefaultDelimiter,
  @arg(flag = 's', doc = "Directly write-out columns that start with these prefixes.") val skipPrefixes: Seq[String] = SkipLinePrefixes,
  @arg(flag = 'x', doc = "Drop records which do not have a mapping.") val drop: Boolean = true
) extends CvBioTool {

  /** Run the tool [[RelabelReferenceNames]]. */
  override def execute(): Unit = {
    val source = Io.toSource(in)
    val writer = Io.toWriter(out)
    val lookup = RelabelReferenceNames.buildMapping(mappingFile, delimiter = DefaultDelimiter)

    source.getLines.zipWithIndex.foreach { case (line: String, i: Int) =>
      if (skipPrefixes.exists(line.startsWith)) {
        writer.write(line + lineSeparator)
      } else {
        val fields  = line.split(delimiter)
        val isValid = columns.map(fields).forall(lookup.keysIterator.contains)
        if (isValid) {
          val patched = patchManyWith(fields, at = columns, using = lookup)
          writer.write(patched.mkString(delimiter.toString) + lineSeparator)
        } else if (drop && !isValid) {
          logger.info(s"A field in record ${i + 1} did not have a mapping: $line")
        } else {
          throw new NoSuchElementException(s"A field in record ${i + 1} did not have a mapping: $line")
        }
      }
    }
    source.safelyClose()
    writer.close()
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
