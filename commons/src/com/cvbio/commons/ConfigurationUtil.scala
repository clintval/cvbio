package com.cvbio.commons

import java.io.File
import java.nio.file.{Files, Path}

import com.cvbio.commons.CommonsDef.FilePath

/** Helper methods for configuration. */
object ConfigurationUtil {
  import com.fulcrumgenomics.commons.io.PathUtil.pathTo

  /** The system PATH variable. */
  val Path: String = sys.env("PATH")

  /** The paths defined within the PATH variable. */
  val SystemPaths: Seq[FilePath] = Path.split(File.pathSeparator).map(pathTo(_))

  /** Searches the system path for an executable and returns the full path, if found. */
  def findExecutableInPath(executable: String): Option[Path] = {
    SystemPaths.map(p => p.resolve(executable)).find(ex => Files.exists(ex))
  }

  /** Run a function when the JVM exits. */
  def runAtShutdown(call: () => Unit): Unit = {
    val thread = new Thread { override def run(): Unit = call() }
    Runtime.getRuntime.addShutdownHook(thread)
  }
}
