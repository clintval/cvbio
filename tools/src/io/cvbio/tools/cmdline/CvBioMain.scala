package io.cvbio.tools.cmdline

import com.fulcrumgenomics.cmdline.FgBioMain

/** Main program for `cvbio`. */
object CvBioMain {

  def main(args: Array[String]): Unit = new CvBioMain().makeItSoAndExit(args)
}

/** An instance of `cvbio`. */
class CvBioMain extends FgBioMain {

  /** The application name. */
  override def name: String = "cvbio"

  /** The packages to include in the commandline. **/
  override protected def packageList: List[String] = List[String]("io.cvbio")
}
