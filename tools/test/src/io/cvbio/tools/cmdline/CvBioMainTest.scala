package io.cvbio.tools.cmdline

import io.cvbio.testing.UnitSpec
import com.fulcrumgenomics.sopt.{arg, clp}

@clp(group=ClpGroups.SamOrBam, description="A test class")
class TestClp(
  @arg(flag='r', doc="Return me!") val exitCode: Option[Int] = Some(10)
) extends CvBioTool {
  override def execute(): Unit = {
    exitCode match {
      case Some(code) => fail(code)
      case None       => fail(exit = 1)
    }
  }
}

class CvBioMainTest extends UnitSpec {

  "CvBioMain" should "execute and issue a help message" in {
    val logs: LoggerString = captureLogger { () =>
      new CvBioMain().makeItSo("TestClp".split(' ')) shouldBe 10
      new CvBioMain().makeItSo("TestClp -r 8".split(' ')) shouldBe 8
    }
  }
}
