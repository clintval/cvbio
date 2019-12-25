package io.cvbio.commons.effectful

import com.fulcrumgenomics.commons.io.PathUtil
import io.cvbio.commons.ConfigurationUtil
import io.cvbio.testing.UnitSpec

import scala.util.{Failure, Success, Try}

class CommandLineToolTest extends UnitSpec {

  "CommandLineTool" should "executables should be available if they can be found on the system path" in {
    captureLogger { () =>
      Seq(Python, Python2, Python3, Rscript).par.foreach { exe =>
        if (ConfigurationUtil.findExecutableInPath(exe.executable).nonEmpty) exe.available shouldBe true
      }
    }
  }

  "CommandLineTool.writeResourceToTempFile" should "write a resource to a temporary file" in {
    val tempFile = CommandLineTool.writeResourceToTempFile(resource = "CommandLineToolTest.R")
    tempFile.toFile.deleteOnExit()
    PathUtil.basename(tempFile, trimExt = false) shouldBe "CommandLineToolTest.R"
    Io.readLines(tempFile).toList shouldBe Seq("stopifnot(require(\"ggplot2\"))")
  }

  "CommandLineTool.execCommand" should "execute a command successfully and return the stdout" in {
    val attempt = CommandLineTool.execCommand(Seq("java", "-version"))
    attempt should be a 'success
    attempt.success.value.exists(_.contains("version")) shouldBe true
  }

  it should "wrap the exception in a Try()" in {
    val attempt = CommandLineTool.execCommand(Seq("java", "-VERSION"))
    attempt should be a 'failure
  }

  "CommandLineTool.available" should "should work for a simple example" in {
    object Java extends CommandLineTool {
      override val executable: String = "java"
      override val testArgs: Seq[String] = Seq("-version")
    }
    captureLogger { () => Java.available shouldBe true }
  }

  "CommandLineTool.Rscript" should "test if ggplot2 is available" in {
    captureLogger { () =>
      if (ConfigurationUtil.findExecutableInPath(Rscript.executable).nonEmpty) {
        Try(Rscript.execIfAvailable(scriptResource = "CommandLineToolTest.R", Seq.empty)) match {
          case Success(_)            => Rscript.ggplot2Available shouldBe true
          case Failure(e: Throwable) => e.getMessage should include ("Cannot execute script")
        }
        Try(Rscript.execIfAvailable(scriptResource = "CommandLineToolFailureTest.R", Seq.empty)) match {
          case Success(_)            => Rscript.moduleAvailable(module = "thisPackageDoesNotExist") shouldBe false
          case Failure(e: Throwable) => e.getMessage should include("Rscript failed with exit code 1.")
        }
      }
    }
  }

  "ScriptRunner" should "execute a script if the executable is available" in {
    val rLogs  = captureLogger { () => Rscript.execIfAvailable(scriptResource = "CommandLineToolTest.R", Seq.empty) }
    if (Rscript.available) rLogs should include ("Loading required package")

    val pyLogs = captureLogger { () => Python.execIfAvailable(scriptResource = "CommandLineToolTest.py", Seq.empty) }
    if (Python.available) pyLogs should include ("Executing CommandLineToolTest.py")
  }

  it should "fail if the script resource does not exist" in {
    captureLogger { () =>
      an[IllegalArgumentException] shouldBe thrownBy { Python.execIfAvailable(scriptResource = "scriptResourceThatDoesNotExist.R", Seq.empty) }
    }
  }

  "Modular" should "test that generic builtins are available in Python" in {
    captureLogger { () =>
      if (ConfigurationUtil.findExecutableInPath(Python.executable).nonEmpty) {
        Python.moduleAvailable("os") shouldBe true
        Python.moduleAvailable(Seq("os", "sys")) shouldBe true
      }
    }
  }

  "Versioned" should "emit the current version of Python" in {
    captureLogger { () =>
      if (ConfigurationUtil.findExecutableInPath(Python.executable).nonEmpty) {
        Python.version shouldBe 'success
        Python.version.success.value.exists(_.contains("Python")) shouldBe true
      }
    }
  }
}
