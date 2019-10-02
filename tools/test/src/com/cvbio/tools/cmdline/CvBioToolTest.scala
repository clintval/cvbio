package com.cvbio.tools.cmdline

import com.cvbio.testing.UnitSpec
import com.fulcrumgenomics.sopt.cmdline.CommandLineProgramParser
import com.fulcrumgenomics.sopt.util.ParsingUtil

class CvBioToolTest extends UnitSpec {

  /** All CLPs that are children of [[CvBioTool]]. */
  private val commandSet = ParsingUtil.findClpClasses[CvBioTool](List("com.cvbio"))

  "CvBio tools" should "find at least one clp" in {
    commandSet.size should be >= 1
  }

  commandSet.foreach { case (clazz, _) =>
    it should s"render a command line for ${clazz.getSimpleName}" in {
      new CommandLineProgramParser(clazz)
    }
  }
}
