package io.cvbio.tools.igv

import java.io.{BufferedReader, Closeable, InputStreamReader, PrintWriter}
import java.net.{InetAddress, Socket}

import com.fulcrumgenomics.FgBioDef.FgBioEnum
import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.commons.util.{CaptureSystemStreams, LazyLogging}
import enumeratum.EnumEntry
import htsjdk.samtools.util.CloserUtil
import io.cvbio.commons.CommonsDef._
import io.cvbio.commons.ConfigurationUtil
import io.cvbio.commons.effectful.Io
import io.cvbio.tools.igv.Igv.IgvResponse

import scala.collection.immutable
import scala.util.Try
import scala.util.Properties.isMac

/** A controller of a currently running IGV application instance.
  *
  * {{{
  *   scala> val igv = new Igv("127.0.0.1", 60151)
  *   scala> igv.ready
  *   true
  *   scala> igv.exec(Echo)
  *   [2019/10/13 22:52:12 | Igv | Info] Executing: echo
  *   [2019/10/13 22:52:25 | Igv | Info] Response: echo
  *   scala> igv.close(andKill = true)
  *   [2019/10/13 22:53:39 | Igv | Info] Executing: exit
  * }}}
  */
class Igv(host: String, port: Int) extends LazyLogging with CaptureSystemStreams with Closeable {

  /** The socket connection to the IGV server. */
  private val socket = {
    logger.info(s"Attempting a socket connection: $host:$port")
    require(Igv.available(host, port), s"Port already in use: $host:$port.")
    new Socket(host, port)
  }

  /** The input reader for <socket>. */
  private val in = new BufferedReader(new InputStreamReader(this.socket.getInputStream))

  /** The output print writer for <socket>. */
  private val out = new PrintWriter(this.socket.getOutputStream, true)

  /** Whether IGV is ready for communication or not. */
  def ready: Boolean = {
    var available: Boolean = false
    captureLogger { () => available = Try(exec(Echo)).toOption.flatten.contains(Echo.toString) }
    available
  }

  /** Execute a command against the current IGV server. */
  private def exec(args: String*): Option[IgvResponse] = {
    val command = args.map(_.trim).mkString(" ")
    logger.info(s"Executing: $command")
    this.out.println(command)
    val response: Option[IgvResponse] = Option(this.in.readLine())
    response.foreach(r => logger.info(s"Response: $r")) // Not all commands respond!
    response
  }

  /** Execute an IGV command.
    *
    * {{{
    *   scala> val igv = new Igv("127.0.0.1", 60151)
    *   scala> igv.exec(Echo)
    *   [2019/10/14 18:07:12 | Igv | Info] Executing: echo
    *   [2019/10/14 18:07:25 | Igv | Info] Response: echo
    *   scala> igv.exec(Goto(new Interval("chr21", 6495564, 6495564)))
    *   [2019/10/14 18:08:01 | Igv | Info] Executing: goto chr21:6495564-6495564
    *   [2019/10/14 18:08:03 | Igv | Info] Response: OK
    *   scala> igv.exec(Exit)
    *   [2019/10/14 18:09:05 | Igv | Info] Executing: exit
    * }}}
    */
  def exec(command: IgvCommand): Option[IgvResponse] = exec(command.toString)

  /** Execute multiple IGV commands.
    *
    * {{{
    *   scala> val igv = new Igv("127.0.0.1", 60151)
    *   scala> igv.exec(Seq(Echo, Goto(new Interval("chr21", 6495564, 6495564)))
    *   [2019/10/14 18:07:12 | Igv | Info] Executing: echo
    *   [2019/10/14 18:07:25 | Igv | Info] Response: echo
    *   [2019/10/14 18:08:01 | Igv | Info] Executing: goto chr21:6495564-6495564
    *   [2019/10/14 18:08:03 | Igv | Info] Response: OK
    * }}}
    */
  def exec(commands: Seq[IgvCommand]): Seq[Option[IgvResponse]] = commands.map(exec)

  /** Closes the socket connection to the IGV server. */
  def close(): Unit = close(andKill = false)

  /** Closes the socket connection to the IGV server.
    *
    * @param andKill Shutdown the IGV application before closing the underlying socket.
    */
  def close(andKill: Boolean = false): Unit = {
    if (this.socket.isConnected && andKill) exec(Exit)
    CloserUtil.close(this.out)
    if (this.socket.isConnected) this.socket.safelyClose()
  }
}


/** Companion object for [[Igv]]. */
object Igv extends LazyLogging {

  /** The IGV response. Often "OK". */
  type IgvResponse = String

  /** The default host IP. */
  lazy val DefaultHost: String = InetAddress.getLoopbackAddress.getHostAddress

  /** The default memory in gigabytes to use when launching IGV. */
  val DefaultMemory: Int = 5

  /** The default port. */
  val DefaultPort: Int = 60151

  /** The time to wait before checking if IGV has booted in milliseconds. */
  private val DefaultWaitTime: Int = 2000

  /** The name of the executable. */
  val Executable: String = "igv"

  /** Check to see if IGV is available. */
  def available(host: String = DefaultHost, port: Int = DefaultPort): Boolean = {
    Try { new Socket(host, port).close() }.isSuccess
  }

  /** Initialize the IGV application from a filepath, if not already running.
    *
    * If the filepath is a JAR file then the JAR will be launched with <jvmMemory>.
    * If the filepath is a MacOS application then the application will be launched accordingly.
    * If the filepath is a path to an executable, then it will be executed.
    */
  def apply(
    path: FilePath,
    jvmMemory: Int = DefaultMemory,
    port: Int = DefaultPort,
    closeOnExit: Boolean = false
  ): Igv = {
    val command = if (PathUtil.extensionOf(path).contains(JarExtension)) {
      Seq("java", s"-Xmx${jvmMemory}m", "-jar", path.toAbsolutePath.toString)
    } else if (isMac && PathUtil.extensionOf(path).contains(MacAppExtension)) {
      Seq("open", path.toAbsolutePath.toString)
    } else {
      Seq(path.toString)
    }
    initialize(command, DefaultHost, port, closeOnExit)
  }

  /** Initialize the IGV application from an executable, if not already running.*/
  def apply(
    executable: String,
    port: Int,
    closeOnExit: Boolean
  ): Igv = {
    ConfigurationUtil.findExecutableInPath(executable) match {
      case None       => throw new IllegalStateException(s"Could not find executable: '$executable'")
      case Some(exec) => apply(exec, port = port, closeOnExit = closeOnExit)
    }
  }

  /** Initialize the IGV application from a command, if not already running. */
  private def initialize(command: Seq[String], host: String, port: Int, closeOnExit: Boolean = false): Igv = {
    if (Igv.available(host, port)) {
      val igv = new Igv(host, port)
      if (closeOnExit) ConfigurationUtil.runAtShutdown(() => igv.close(andKill = true))
      igv
    } else {
      logger.info(s"Initializing IGV with command: ${command.mkString(" ")}")
      val process = new ProcessBuilder(command: _*).start()
      val pipe1   = Io.pipeStream(process.getErrorStream, logger.info)
      val pipe2   = Io.pipeStream(process.getInputStream, logger.debug)

      do {
        logger.info(s"Waiting for a socket connection: $host:$port")
        Thread.sleep(DefaultWaitTime)  // TODO: Need to timeout, especially if port is incorrect.
      } while (process.isAlive || process.exitValue() == 0 && !available(host, port))

      if (!process.isAlive) require(process.exitValue() == 0, "Could not initialize IGV.")

      val igv = new Igv(host, port)

      if (closeOnExit) {
        ConfigurationUtil.runAtShutdown(() => igv.close(andKill = true))
        ConfigurationUtil.runAtShutdown(() => process.destroy())
        ConfigurationUtil.runAtShutdown(() => pipe1.close())
        ConfigurationUtil.runAtShutdown(() => pipe2.close())
      }

      igv
    }
  }

  /** Find the IGV executable */

  /** Trait that all enumerations of [[OutputFormat]] must extend. */
  sealed trait OutputFormat extends EnumEntry { def suffix: FilenameSuffix }

  /** The supported image output formats. */
  object OutputFormat extends FgBioEnum[OutputFormat] {

    /** All values of [[OutputFormat]]. */
    override def values: immutable.IndexedSeq[OutputFormat] = findValues

    /** The SVG output format. */
    case object Svg extends OutputFormat { val suffix = ".svg" }

    /** The PNG output format. */
    case object Png extends OutputFormat { val suffix = ".png" }

    /** The JPEG output format. */
    case object Jpg extends OutputFormat { val suffix = ".jpg" }
  }
}
