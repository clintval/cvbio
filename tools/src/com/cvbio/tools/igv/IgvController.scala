package com.cvbio.tools.igv

import java.io.{BufferedReader, Closeable, InputStreamReader, PrintWriter}
import java.net.Socket

import com.cvbio.commons.CommonsDef._
import com.fulcrumgenomics.commons.util.{CaptureSystemStreams, LazyLogging}
import htsjdk.samtools.util.CloserUtil

import scala.util.Try


/** Common defaults and functions for [[IgvController]]. */
object IgvController {
  type IgvResponse = String
}

/** A class to control an IGV application.
  *
  * {{{
  *   scala> val igv = new IgvController("127.0.0.1", 60151)
  *   scala> igv.available
  *   true
  *   scala> igv.close(andKill = true)
  *   [2019/10/13 22:53:39 | IgvController | Info] Executing: exit
  * }}}
  */
class IgvController(host: String, port: Int) extends LazyLogging with CaptureSystemStreams with Closeable {
  import IgvController._

  /** The socket connection to the IGV server. */
  private val socket = new Socket(host, port)

  /** The input reader for <socket>. */
  private val in = new BufferedReader(new InputStreamReader(this.socket.getInputStream))

  /** The output print writer for <socket>. */
  private val out = new PrintWriter(this.socket.getOutputStream, true)

  /** Whether IGV is available or not. */
  def available: Boolean = {
    var available: Boolean = false
    captureLogger { () => available = Try(exec(Echo)).toOption.flatten.contains(Echo.toString) }
    available
  }

  /** Execute a sequence of arguments against the current IGV server. */
  private def exec(args: String*): Option[IgvResponse] = {
    val command = args.map(_.trim).mkString(" ")
    logger.info(s"Executing: $command")
    this.out.println(command)
    val response: Option[IgvResponse] = Option(this.in.readLine())
    response.foreach(r => logger.info(s"Response: $r")) // Not all commands respond
    response
  }

  /** Execute an IGV command. */
  def exec(command: IgvCommand): Option[IgvResponse] = exec(command.toString)

  /** Closes the socket connection to the IGV server. */
  def close(): Unit = close(false)

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
