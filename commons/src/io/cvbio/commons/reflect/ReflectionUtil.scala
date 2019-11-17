package io.cvbio.commons.reflect

import java.util.jar.JarFile

import io.cvbio.commons.CommonsDef._

import scala.io.Source

object ReflectionUtil {

  /** List all the resources in the code source where this class is found. */
  def resourceListing[T](clazz: Class[T]): Seq[String] = {
    clazz.getProtectionDomain.getCodeSource.getLocation.toURI match {
      case loc if loc.getScheme == "file" && loc.toASCIIString.endsWith( ".jar") => {
        val entries  = new JarFile(loc.getPath).entries()
        val iterator = new Iterator[String] {
          override def hasNext: Boolean = entries.hasMoreElements
          override def next(): String   = entries.nextElement().toString
        }
        iterator.toList
      }
      case loc => throw new IllegalStateException(s"Unknown resource object: $loc")
    }
  }

  /** Stream a resource object to lines. */
  def getResourceAsLines(path: Filename): Iterator[String] = {
    val stream = getClass.getResourceAsStream(path)
    Source.fromInputStream(stream).withClose(() => stream.close()).getLines
  }

  /** Given a path prefix to a file, and a filename, stream the resource object to lines. */
  def getResourceAsLines(prefix: FilenamePrefix, filename: Filename): Iterator[String] = {
    getResourceAsLines(prefix + "/" + filename)
  }
}
