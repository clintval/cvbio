package com.cvbio.tools.igv

import java.io.{BufferedReader, Closeable, InputStreamReader, PrintWriter}
import java.net.Socket

import com.cvbio.commons.CommonsDef._
import com.fulcrumgenomics.commons.util.LazyLogging
import htsjdk.samtools.util.CloserUtil

import scala.util.Try


/** Common defaults and functions for [[IgvController]]. */
object IgvController {
  type IgvResponse = String
}

/** A class to control an IGV application. */
class IgvController(host: String, port: Int) extends LazyLogging with Closeable {
  import IgvController._

  /** The socket connection to the IGV server. */
  private val socket = new Socket(host, port)

  /** The input reader for <socket>. */
  private val in = new BufferedReader(new InputStreamReader(this.socket.getInputStream))

  /** The output print writer for <socket>. */
  private val out = new PrintWriter(this.socket.getOutputStream, true)

  /** Whether IGV is available or not. */
  def available: Boolean = Try(exec("echo")).toOption.flatten.contains("echo")

  /** Execute a sequence of arguments against the current IGV server. */
  private def exec(args: String*): Option[IgvResponse] = {
    val command = args.map(_.trim).mkString(" ")
    logger.debug(s"Executing: $command")
    this.out.println(command)
    val response: Option[IgvResponse] = Option(this.in.readLine())
    response.foreach(r => logger.debug(s"Response: $r")) // Not all commands respond
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
