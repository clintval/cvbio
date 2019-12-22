package io.cvbio.commons

import java.io.File
import java.nio.file.{FileVisitOption, Files, Path, Paths}

import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.commons.io.PathUtil.{basename, extensionOf, pathTo}
import io.cvbio.commons.CommonsDef._
import com.fulcrumgenomics.commons.io.PathUtil.pathTo

import scala.util.Properties.isMac

/** Helper methods for configuration. */
object ConfigurationUtil {

  /** The system PATH variable. */
  lazy val Path: String = sys.env("PATH")

  /** The root of this Unix system. */
  val Root: Path = PathUtil.pathTo("/")

  /** The paths defined within the PATH variable. */
  lazy val SystemPaths: Seq[FilePath] = Path.split(File.pathSeparator).map(pathTo(_))

  /** The location on Mac operating systems where applications are installed. */
  val MacApplicationRoot: DirPath = Root.resolve("Applications")

  /** Searches the system path for an executable and returns the full path, if found. */
  def findExecutableInPath(executable: String): Option[Path] = {
    SystemPaths.map(p => p.resolve(executable)).find(ex => Files.exists(ex))
  }

  /** Search the application directory for an App that starts with <prefix>. */
  def findMacApplication(prefix: String): Option[PathToMacApp] = {
    if (!isMac) { None }
    else {
      Files.walk(MacApplicationRoot, 1)
        .iterator
        .find(path => basename(path).startsWith(prefix) && extensionOf(path).contains(MacAppExtension))
    }
  }

  /** Run a function when the JVM exits. */
  def runAtShutdown[T](call: () => T): Unit = {
    val thread = new Thread { override def run(): Unit = call() }
    Runtime.getRuntime.addShutdownHook(thread)
  }
}
