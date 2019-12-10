package io.cvbio.tools.util

import com.fulcrumgenomics.sopt._
import io.cvbio.commons.CommonsDef._
import io.cvbio.tools.cmdline.ClpGroups

@deprecated(message = "Use `UpdateDataContigNames` instead.", since = "2.0.0")
@clp(
  description =
    """
      |Relabel reference sequence names in delimited data using a chromosome name mapping table.
      |
      |A collection of mapping tables is maintained at the following location:
      |
      | - https://github.com/dpryan79/ChromosomeMappings
    """,
  group = ClpGroups.Deprecated
) class RelabelReferenceNames(
  @arg(flag = 'i', doc = "The input file.") override val in: FilePath,
  @arg(flag = 'o', doc = "The output file.") override val out: FilePath,
  @arg(flag = 'm', doc = "A two-column tab-delimited mapping file.") val mappingFile: FilePath,
  @arg(flag = 'c', doc = "The column names to convert, 0-indexed.", minElements = 1) override val columns: Seq[Int] = Seq(0),
  @arg(flag = 'd', doc = "The input file data delimiter.") override val delimiter: Char = '\t',
  @arg(flag = 's', doc = "Directly write-out columns that start with these prefixes.") val skipPrefixes: Seq[String] = Seq("#"),
  @arg(flag = 'x', doc = "Drop records which do not have a mapping.") val drop: Boolean = true
) extends UpdateDataContigNames(in, out, mappingFile, columns, delimiter, skipPrefixes, drop) {

  /** Run the tool [[RelabelReferenceNames]]. */
  override def execute(): Unit = {
    logger.warning("This tool is deprecated, please use `UpdateDataContigNames` instead.")
    super.execute()
  }
}
