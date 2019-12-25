package io.cvbio.commons.reflect

import java.nio.file.Files
import java.util.jar.JarFile

import com.fulcrumgenomics.commons.io.PathUtil
import io.cvbio.commons.CommonsDef._

import scala.collection.JavaConverters._

/** Helper methods for reflective code. */
object ReflectionUtil {

  /** The scheme component of the URI for files. */
  private val FileScheme: String = "file"

  /** List all the resources in the code source where this class is found. */
  def resourceListing[T](clazz: Class[T]): Iterator[String] = {
    clazz.getProtectionDomain.getCodeSource.getLocation.toURI match {
      case loc if loc.getScheme == FileScheme && loc.toASCIIString.endsWith(JarExtension) => {
        val entries  = new JarFile(loc.getPath).entries()
        val iterator = new Iterator[String] {
          override def hasNext: Boolean = entries.hasMoreElements
          override def next: String     = entries.nextElement.toString
        }
        iterator
      }
      case loc if loc.getScheme == FileScheme && PathUtil.pathTo(loc.getPath).toFile.isDirectory => {
        Files.walk(PathUtil.pathTo(loc.getPath)).iterator.asScala.filter(Files.isRegularFile(_)).map(_.toString)
      }
      case loc => throw new IllegalStateException(s"Unknown resource object: $loc")
    }
  }
}
