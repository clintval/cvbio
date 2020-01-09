package io.cvbio.tools.util

import com.fulcrumgenomics.commons.util.DelimitedDataParser
import com.fulcrumgenomics.sopt._
import com.fulcrumgenomics.util.{Io, ProgressLogger}
import io.cvbio.commons.CommonsDef._
import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import io.cvbio.tools.util.UpdateContigNames.{CommentLinePrefixes, DefaultDelimiter}

import scala.util.Properties.lineSeparator

@clp(
  description =
    """
      |Update contig names in delimited data using a name mapping table.
      |
      |A collection of mapping tables is maintained at the following location:
      |
      | - https://github.com/dpryan79/ChromosomeMappings
    """,
  group = ClpGroups.Util
) class UpdateContigNames(
  @arg(flag = 'i', doc = "The input file.") val in: FilePath,
  @arg(flag = 'o', doc = "The output file.") val out: FilePath,
  @arg(flag = 'm', doc = "A two-column tab-delimited mapping file.") val mapping: FilePath,
  @arg(flag = 'c', doc = "The column names to convert, 0-indexed.", minElements = 1) val columns: Seq[Int] = Seq(0),
  @arg(flag = 'd', doc = "The input file data delimiter.") val delimiter: Char = DefaultDelimiter,
  @arg(flag = 'C', doc = "Directly write-out lines that start with these prefixes.") val commentChars: Seq[String] = CommentLinePrefixes,
  @arg(flag = 's', doc = "Skip (ignore) records which do not have a mapping.") val skipMissing: Boolean = true
) extends CvBioTool {

  /** Run the tool [[UpdateContigNames]]. */
  override def execute(): Unit = {
    val source   = Io.toSource(in)
    val writer   = Io.toWriter(out)
    val progress = ProgressLogger(logger)
    val lookup   = UpdateContigNames.buildMapping(mapping, delimiter = DefaultDelimiter)

    source.getLines.zipWithIndex.foreach { case (line: String, i: Int) =>
      if (commentChars.exists(line.startsWith)) {
        writer.write(line + lineSeparator)
      } else {
        val fields  = line.split(delimiter)
        val isValid = columns.map(fields).forall(lookup.keysIterator.contains)
        if (isValid) {
          val patched = patchManyWith(fields, at = columns, using = lookup)
          writer.write(patched.mkString(delimiter.toString) + lineSeparator)
        } else if (skipMissing && !isValid) {
          logger.info(s"A field in record ${i + 1} did not have a mapping: $line")
        } else {
          throw new NoSuchElementException(s"A field in record ${i + 1} did not have a mapping: $line")
        }
      }
      progress.record()
    }
    progress.logLast()
    source.safelyClose()
    writer.close()
  }
}

/** Companion object for [[UpdateContigNames]]. */
object UpdateContigNames {

  /** The default delimiter to use for most text files. */
  val DefaultDelimiter: Char = '\t'

  /** Skip lines with these prefixes and treat them as comments. */
  val CommentLinePrefixes: Seq[String] = Seq("#")

  /** Build a reference sequence name mapping. */
  private[util] def buildMapping(lines: Iterator[String], delimiter: Char = DefaultDelimiter): Map[String, String] = {
    val rows = new DelimitedDataParser(lines, delimiter = delimiter, header = Seq("1", "2"))
    rows.map { row => row[String](index = 0) -> row[String](index = 1) }.toMap
  }

  /** Build a reference sequence name mapping. */
  private[util] def buildMapping(file: FilePath, delimiter: Char): Map[String, String] = {
    buildMapping(Io.toSource(file).getLines(), delimiter = delimiter)
  }

  /** Update a line of delimited data using the function <using> for each index in <at>.
    *
    * @param line a sequence of delimited data
    * @param at the indexes (0-based) of <line> split by <delimiter> where we will update contig/chromosome names
    * @param delimiter the delimiter for the data in <line>
    * @param using a method for updating contig/chromosome names
    */
  def update(line: String, at: Seq[Int], delimiter: Char = DefaultDelimiter, using: String => String): String = {
    val patched = patchManyWith(line.split(delimiter), at, using)
    patched.mkString(delimiter.toString)
  }
}
