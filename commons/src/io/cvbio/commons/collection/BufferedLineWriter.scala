package io.cvbio.commons.collection

import io.cvbio.commons.CommonsDef._
import java.io.BufferedWriter

/** Enable a [[java.io.BufferedWriter]] to write a complete line. */
trait BufferedLineWriter extends BufferedWriter {

  /** Write a string and then suffix with a newline separator to form a proper line. */
  def writeLine(str: String): Unit = yieldAndThen(write(str))(newLine())
}
