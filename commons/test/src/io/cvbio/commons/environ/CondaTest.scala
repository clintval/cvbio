package io.cvbio.commons.environ

import io.cvbio.commons.effectful.CommandLineTool.ToolException
import io.cvbio.commons.environ.Conda.PackageInfo
import io.cvbio.testing.UnitSpec
import spray.json._

import scala.util.{Failure, Success}

class CondaTest extends UnitSpec {

  "Conda.version" should "report the version when conda is installed" in {
    captureLogger { () =>
      Conda.version match {
        case Success(version) => version.mkString("") should include("conda")
        case Failure(e: ToolException) if Conda.available => throw new IllegalStateException("Builtins should be installed.")
        case Failure(e: ToolException) => Unit
        case Failure(e: Throwable)     => throw e
      }
    }
  }

  "Conda.moduleAvailable" should "find a generic package when conda is installed" in {
    captureLogger { () => Conda.moduleAvailable("pip") shouldBe Conda.available }
  }

  it should "find multiple generic packages when conda is installed" in {
    captureLogger { () => Conda.moduleAvailable(Seq("conda", "pip")) shouldBe Conda.available }
  }

  "Conda.PackageInfoJsonProtocol" should "round trip a PackageInfo class" in {
    import io.cvbio.commons.environ.Conda.PackageInfoJsonProtocol._
    val info = PackageInfo("http://baseurl.com", 2, "2", "bioconda", "macosx", "pip", "x86", "0.3.3")
    info.toJson.convertTo[PackageInfo] shouldBe info
  }
}
