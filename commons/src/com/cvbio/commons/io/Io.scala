package com.cvbio.commons.io

import com.cvbio.commons.CommonsDef._
import com.fulcrumgenomics.commons.io.IoUtil

import scala.util.Try

/** Helper methods and defaults for working with IO. */
class Io extends IoUtil {

  /** Test if a filepath is readable. */
  def readable(path: FilePath): Boolean = Try(assertReadable(path)).isSuccess

  /** Test if a filepath is writable. */
  def writable(path: FilePath): Boolean = Try(assertCanWriteFile(path)).isSuccess
}

/** Singleton extending our IO utilities. */
object Io extends Io()
