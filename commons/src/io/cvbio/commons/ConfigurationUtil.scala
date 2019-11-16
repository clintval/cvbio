package io.cvbio.commons

import java.io.File
import java.nio.file.{Files, Path}

import io.cvbio.commons.CommonsDef.FilePath
import com.fulcrumgenomics.commons.io.PathUtil.pathTo

/** Helper methods for configuration. */
object ConfigurationUtil {

  /** The system PATH variable. */
  lazy val Path: String = sys.env("PATH")

  /** The paths defined within the PATH variable. */
  lazy val SystemPaths: Seq[FilePath] = Path.split(File.pathSeparator).map(pathTo(_))

  /** Searches the system path for an executable and returns the full path, if found. */
  def findExecutableInPath(executable: String): Option[Path] = {
    SystemPaths.map(p => p.resolve(executable)).find(ex => Files.exists(ex))
  }

  /** Run a function when the JVM exits. */
  def runAtShutdown[T](call: () => T): Unit = {
    val thread = new Thread { override def run(): Unit = call() }
    Runtime.getRuntime.addShutdownHook(thread)
  }
}
