package com.cvbio.tools.igv

/** An abstraction over a running IGV application. */
class Igv(host: String, port: Int) {
  private val controller = new IgvController(host, port)
  def runPlay(play: IgvPlay): Unit = play.foreach(controller.exec)
  def kill(): Unit = controller.close(andKill = true)
}
