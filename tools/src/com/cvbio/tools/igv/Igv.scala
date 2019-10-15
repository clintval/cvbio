package com.cvbio.tools.igv

import java.io.Closeable

import com.cvbio.commons.CommonsDef.FilenameSuffix

/** An interface to a running IGV application. */
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

  /** Valid output suffixes for snapshot file paths. */
  val ValidOutputSuffixes: Seq[FilenameSuffix] = Seq(".png", ".jpg", ".svg")
}
