package io.cvbio.commons.effectful

import java.io.{BufferedWriter, InputStream, OutputStreamWriter}
import java.nio.file.Path

import io.cvbio.commons.CommonsDef._
import io.cvbio.commons.collection.{BufferedLineWriter, Md5CalculatingInputStream}
import com.fulcrumgenomics.commons.io.IoUtil

import scala.io.Source
import scala.util.Try

/** Helper methods and defaults for working with IO. */
class Io extends IoUtil {

  /** Test if a filepath is readable. */
  def readable(path: FilePath): Boolean = Try(assertReadable(path)).isSuccess

  /** Test if a filepath is writable. */
  def writable(path: FilePath): Boolean = Try(assertCanWriteFile(path)).isSuccess

  /** Creates a new BufferedWriter LineWriter to write to the supplied path. */
  def toLineWriter(path: Path): BufferedLineWriter = new BufferedWriter(new OutputStreamWriter(toOutputStream(path)), bufferSize) with BufferedLineWriter

  /** Calculates an MD5 checksum on the file. */
  def md5(path: FilePath): String = md5(Io.toInputStream(path))

  /** Calculates an MD5 checksum on the entire input stream. */
  def md5(inputStream: InputStream): String = {
    val stream = new Md5CalculatingInputStream(inputStream)
    Source.fromInputStream(inputStream).foreach(locally)
    stream.hash.getOrElse(unreachable("We should have consumed the entire stream."))
  }
}

/** Singleton extending our IO utilities. */
object Io extends Io()
