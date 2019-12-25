package io.cvbio.commons.effectful
// TODO: Replace with this PR, once merged: https://github.com/fulcrumgenomics/commons/pull/58/files

import java.nio.file.Path

import com.fulcrumgenomics.commons.CommonsDef._
import com.fulcrumgenomics.commons.io.{AsyncStreamSink, PathUtil}
import com.fulcrumgenomics.commons.util.{LazyLogging, Logger}
import io.cvbio.commons.effectful.CommandLineTool.ToolException

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/** An abstraction over a command line tool to make executing that tool easier. */
trait CommandLineTool extends LazyLogging {

  /** The name of the executable. */
  val executable: String

  /** The arguments to use for testing a correct installation of an executable. */
  val testArgs: Seq[String]

  /** Returns true if the executable is available and false otherwise. */
  lazy val available: Boolean = CommandLineTool.execCommand(executable +: testArgs, Some(logger)).isSuccess
}

/** A mixin for executables that can run scripts. */
trait ScriptRunner {
  self: CommandLineTool =>

  /** The suffix of scripts that can be run by this executable. */
  val suffix: FilenameSuffix

  /** Executes a script from the classpath if this executable is available.
    *
    * @param scriptResource the name of the script resource on the classpath
    * @param args a list of arguments to pass to the script
    * @throws Exception when the script on the classpath with the given arguments cannot be executed successfully
    */
  def execIfAvailable(scriptResource: String, args: Seq[String]): Unit = {
    if (available) Try(exec(scriptResource, args)) match {
      case Success(_)            => Unit
      case Failure(e: Throwable) =>
        logger.error( s"Cannot execute script from $scriptResource with args: ${args.mkString(" ")}")
        throw e
    }
  }

  /** Executes a script stored at a Path if the this executable is available.
    *
    * @param path filepath of the script to be run
    * @param args a list of arguments to pass to the script
    * @throws Exception when the script at the path with the given arguments cannot be executed successfully
    */
  def execIfAvailable(path: Path, args: Seq[String]): Unit = {
    if (available) Try(exec(path, args)) match {
      case Success(_)            => Unit
      case Failure(e: Throwable) =>
        logger.error( s"Cannot execute script $path with args: ${args.mkString(" ")}")
        throw e
    }
  }

  /** Executes a script from the classpath, raise an exception otherwise.
    *
    * @param scriptResource the name of the script resource on the classpath
    * @param args a list of arguments to pass to the script
    * @throws Exception when the script on the classpath with the given arguments cannot be executed successfully
    */
  def exec(scriptResource: String, args: Seq[String]): Unit = exec(CommandLineTool.writeResourceToTempFile(scriptResource), args)

  /** Executes a script from the filesystem Path.
    *
    * @param path Path to the script to be executed
    * @param args a variable list of arguments to pass to the script
    * @throws ToolException when the exit code from the called process is not zero
    */
  def exec(path: Path, args: Seq[String]): Unit = {
    val basename = PathUtil.basename(path, trimExt = false)
    logger.info(s"Executing $basename with $executable using the arguments: ${args.mkString(" ")}")
    val command = executable +: path.toAbsolutePath.toString +: args
    val process = new ProcessBuilder(command: _*).redirectErrorStream(false).start()
    val pipe1   = Io.pipeStream(process.getErrorStream, logger.warning)
    val pipe2   = Io.pipeStream(process.getInputStream, logger.info)
    val retval  = process.waitFor()

    pipe1.close()
    pipe2.close()

    if (retval != 0) throw ToolException(executable, retval)
  }
}

/** Defines values used to get the version of the executable. */
trait Versioned {
  self: CommandLineTool =>

  /** The version flag. */
  val versionFlag: String = "--version"

  /** Use the version flag to test the successful install of the executable. */
  val testArgs: Seq[String] = Seq(versionFlag)

  /** Returns the version string of this executable. */
  lazy val version: Try[ListBuffer[String]] = CommandLineTool.execCommand(executable +: testArgs, Some(logger))
}

/** A mixin for executables that have packages or modules. */
trait Modular {
  self: CommandLineTool =>

  /** The command to use for testing a module's existence. */
  def testModuleCommand(module: String): Seq[String]

  /** Returns true if the tested module exist with the tested executable. */
  def moduleAvailable(module: String): Boolean = {
    if (self.available) CommandLineTool.execCommand(testModuleCommand(module), Some(logger)).isSuccess
    else false
  }

  /** Returns true if all tested modules exist with the tested executable.
    *
    * For example:
    * {{
    * scala> import com.cvbio.commons.effectful._
    * scala> Rscript.moduleAvailable(Seq("ggplot2", "dplyr"))
    * res1: Boolean = true
    * }}
    */
  def moduleAvailable(modules: Seq[String]): Boolean = !modules.par.map(moduleAvailable).exists(_ == false)
}

/** Companion object for [[CommandLineTool]]. */
object CommandLineTool {

  /** Exception class that holds onto the exit/status code of the executable. */
  case class ToolException(executable: String, status: Int) extends RuntimeException {
    override def getMessage: String = s"$executable failed with exit code $status."
  }

  /** Executes a command and returns the stdout.
    *
    * @param command the command to be executed
    * @param logger an optional logger to use for emitting a status update on initial execution
    */
  def execCommand(command: Seq[String], logger: Option[Logger] = None): Try[ListBuffer[String]] = {
    logger.foreach(_.info(s"Executing: ${command.mkString(" ")}"))
    Try {
      val process  = new ProcessBuilder(command: _*).redirectErrorStream(true).start()
      val stdout   = ListBuffer[String]()
      val sink     = new AsyncStreamSink(process.getInputStream, s => stdout.append(s))
      val exitCode = process.waitFor()

      require(exitCode == 0, s"Command did not execute successfully. Exit code: $exitCode")
      yieldAndThen(stdout)(sink.safelyClose())
    }
  }

  /** Extracts a resource from the classpath and writes it to a temp file on disk.
    *
    * @param resource a given name on the classpath
    */
  private[effectful] def writeResourceToTempFile(resource: String): Path = {
    val lines = Io.readLinesFromResource(resource)
    val dir   = Io.makeTempDir(PathUtil.sanitizeFileName(this.getClass.getSimpleName))
    val path  = PathUtil.pathTo(dir.toString, PathUtil.basename(resource, trimExt = false))
    Seq(dir, path).foreach(_.toFile.deleteOnExit())
    Io.writeLines(path, lines)
    path
  }
}

/** A Python base trait for the various versions of python executables. */
trait Python extends CommandLineTool with Versioned with Modular with ScriptRunner {

  /** The file extension for Python files. */
  val suffix: FilenameSuffix = ".py"

  /** The command to use to test the existence of a Python module. */
  def testModuleCommand(module: String): Seq[String] = Seq(executable, "-c", s"import $module")
}

/** A collection of values and methods specific for the Rscript executable */
object Rscript extends CommandLineTool with Versioned with Modular with ScriptRunner {

  /** The Rscript executable name. */
  val executable: String = "Rscript"

  /** The file extension for Rscript files. */
  val suffix: FilenameSuffix = ".R"

  /** The command to use to test the existence of a Rscript package. */
  def testModuleCommand(module: String): Seq[String] = Seq(executable, "-e", s"stopifnot(require('$module'))")

  /** Only returns true if both Rscript is available and the library `ggplot2` is installed. */
  lazy val ggplot2Available: Boolean = available && moduleAvailable("ggplot2")
}

/** The system Python version. */
object Python extends Python  { val executable: String = "python" }

/** The system Python 2. */
object Python2 extends Python { val executable: String = "python2" }

/** The system Python3. */
object Python3 extends Python { val executable: String = "python3" }
