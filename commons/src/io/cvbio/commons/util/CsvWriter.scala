package io.cvbio.commons.util

import java.io.{BufferedWriter, Closeable, Flushable}
import java.nio.file.Path

import com.fulcrumgenomics.FgBioDef.FgBioEnum
import com.fulcrumgenomics.commons.io.Writer
import enumeratum.EnumEntry
import io.cvbio.commons.effectful.Io
import io.cvbio.commons.util.CsvWriter.Comma

import scala.util.Properties.lineSeparator

/** Implements a writer of comma-separated values. */
class CsvWriter private(val out: BufferedWriter, delimiter: String = Comma)
  extends Closeable with Writer[Seq[Any]] with Flushable {

  /** Writes a series of values to the output. Do not perform any fancy quoting. */
  override def write(recs: Seq[Any]): Unit = {
    out.append(recs.mkString(delimiter) + lineSeparator)
  }

  /** Flush the underlying buffer. */
  override def flush(): Unit = out.flush()

  /** Closes the underlying writer. */
  override def close(): Unit = out.close()
}

/** Companion object for [[CsvWriter]]. */
object CsvWriter {

  /** A comma. */
  val Comma: String = ","

  /** Construct a [[CsvWriter]] that will write to the provided path. */
  def apply(path: Path, delimiter: String = Comma): CsvWriter = apply(Io.toWriter(path), delimiter = delimiter)

  /** Constructs a [[CsvWriter]] from a [[java.io.Writer]]. */
  def apply(writer: java.io.Writer, delimiter: String): CsvWriter = writer match {
    case bw: BufferedWriter => new CsvWriter(bw, delimiter = delimiter)
    case w                  => new CsvWriter(new BufferedWriter(w), delimiter = delimiter)
  }

  /** Trait that all enumeration values of type [[QuoteType]] should extend. */
  sealed trait QuoteType extends EnumEntry

  /** Contains enumerations of quote types. */
  object QuoteType extends FgBioEnum[QuoteType] {

    def values: scala.collection.immutable.IndexedSeq[QuoteType] = findValues

    /** The value when [[QuoteType]] is to quote all fields. */
    case object All extends QuoteType

    /** The value when [[QuoteType]] is to quote only those fields with special characters. */
    case object Minimal extends QuoteType

    /** The value when [[QuoteType]] is to quote all non-numeric fields. */
    case object NonNumeric extends QuoteType

    /** The value when [[QuoteType]] is to perform no quoting at all. */
    case object NoQuoting extends QuoteType
  }
}
