package io.cvbio.tools.util

import com.fulcrumgenomics.sopt.{arg, clp}
import io.cvbio.commons.CommonsDef.FilePath
import io.cvbio.tools.cmdline.ClpGroups
import io.cvbio.tools.util.UpdateContigNames.{CommentLinePrefixes, DefaultDelimiter}

@deprecated("Use `UpdateContigNames` instead", since = "2.2.0")
@clp(
  description =
    """
      |Deprecated: Update contig names in delimited data using a name mapping table.
      |
      |A collection of mapping tables is maintained at the following location:
      |
      | - https://github.com/dpryan79/ChromosomeMappings
    """,
  group = ClpGroups.Deprecated
) class UpdateDataContigNames(
  @arg(flag = 'i', doc = "The input file.") override val in: FilePath,
  @arg(flag = 'o', doc = "The output file.") override val out: FilePath,
  @arg(flag = 'm', doc = "A two-column tab-delimited mapping file.") override val mapping: FilePath,
  @arg(flag = 'c', doc = "The column names to convert, 0-indexed.", minElements = 1) override val columns: Seq[Int] = Seq(0),
  @arg(flag = 'd', doc = "The input file data delimiter.") override val delimiter: Char = DefaultDelimiter,
  @arg(flag = 'C', doc = "Directly write-out lines that start with these prefixes.") override val commentChars: Seq[String] = CommentLinePrefixes,
  @arg(flag = 's', doc = "Skip (ignore) records which do not have a mapping.") override val skipMissing: Boolean = true
) extends UpdateContigNames(
  in           = in,
  out          = out,
  mapping      = mapping,
  columns      = columns,
  delimiter    = delimiter,
  commentChars = commentChars,
  skipMissing  = skipMissing
)
