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

  /** Implicit class that adds a small amount of DSL over [[FilePath]] instances. */
  implicit class PathUtil(private val path: FilePath) {

    /** Concatenate a string onto a filepath. */
    def +(suffix: FilenameSuffix): FilePath = path.getParent.resolve(path.getFileName.toString + suffix)

    /** Resolve a path string against this filepath. */
    def /(other: String): FilePath = path.resolve(other)
  }
}

/** Singleton extending our IO utilities. */
object Io extends Io()