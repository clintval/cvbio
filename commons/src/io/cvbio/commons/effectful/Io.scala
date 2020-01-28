package io.cvbio.commons.effectful

import java.io.{BufferedWriter, OutputStreamWriter}
import java.nio.file.{Path, Paths}

import com.fulcrumgenomics.commons.CommonsDef.DirPath
import io.cvbio.commons.CommonsDef._
import io.cvbio.commons.collection.BufferedLineWriter
import com.fulcrumgenomics.commons.io.{IoUtil, PathUtil}

import scala.util.Try

/** Helper methods and defaults for working with IO. */
class Io extends IoUtil {

  /** The default temp. directory. */
  val tmpDir: DirPath = PathUtil.pathTo(System.getProperty("java.io.tmpdir"))

  /** Test if a filepath is readable. */
  def readable(path: FilePath): Boolean = Try(assertReadable(path)).isSuccess

  /** Test if a filepath is writable. */
  def writable(path: FilePath): Boolean = Try(assertCanWriteFile(path)).isSuccess

  /** Creates a new BufferedWriter LineWriter to write to the supplied path. */
  def toLineWriter(path: Path): BufferedLineWriter = new BufferedWriter(new OutputStreamWriter(toOutputStream(path)), bufferSize) with BufferedLineWriter
}

/** Singleton extending our IO utilities. */
object Io extends Io()
