package com.cvbio.tools.igv

import java.io.Closeable

/** An abstraction over a running IGV application. */
class Igv(host: String, port: Int) extends Closeable {
  private val controller = new IgvController(host, port)
  def runPlay(play: IgvPlay): Unit = play.toList.foreach(controller.exec)
  def close(): Unit = controller.close()
  def close(andKill: Boolean = true): Unit = controller.close(andKill = true)
}

/** Companion object for [[Igv]]. */
object Igv {

  /** The default host IP. */
  val DefaultHost: String = "127.0.0.1"

  /** The default port. */
  val DefaultPort: Int = 60151
}