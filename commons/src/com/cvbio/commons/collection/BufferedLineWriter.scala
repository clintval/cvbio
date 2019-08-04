package com.cvbio.commons.collection

import com.cvbio.commons.CommonsDef._
import java.io.BufferedWriter

/** Enable a [[BufferedWriter]] to write a complete line. */
trait BufferedLineWriter extends BufferedWriter {

  /** Write a string and then suffix with a newline separator to form a proper line. */
  def writeLine(str: String): Unit = yieldAndThen(write(str))(newLine())
}
