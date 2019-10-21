package io.cvbio.commons.reflect

import io.cvbio.commons.CommonsDef._
import scala.io.Source

object ReflectionUtil {

  /** Stream a resource object to lines. */
  private def getResourceAsLines(path: Filename): Iterator[String] = {
    val stream = getClass.getResourceAsStream(path)
    Source.fromInputStream(stream).withClose(() => stream.close()).getLines
  }

  /** Given a path prefix to a file, and a filename, stream the resource object to lines. */
  private def getResourceAsLines(prefix: FilenamePrefix, filename: Filename): Iterator[String] = {
    getResourceAsLines(prefix + "/" + filename)
  }
}
