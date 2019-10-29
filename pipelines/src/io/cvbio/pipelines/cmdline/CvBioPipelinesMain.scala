package io.cvbio.pipelines.cmdline

import dagr.core.cmdline.DagrCoreMain


class CvBioPipelinesMain extends DagrCoreMain {

  /** The application name. */
  override def name: String = "cvbio-pipelines"
}


/** Main program for `cvbio-pipelines`. */
object CvBioPipelinesMain {

  /** The packages to include in the commandline. */
  protected def getPackageList: List[String] = List[String]("io.cvbio")

  /** The main method. */
  def main(args: Array[String]): Unit = {
    System.exit(new CvBioPipelinesMain().makeItSo(args, packageList = getPackageList))
  }
}
