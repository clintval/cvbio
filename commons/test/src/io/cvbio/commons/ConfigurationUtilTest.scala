package io.cvbio.commons

import com.fulcrumgenomics.commons.io.PathUtil
import io.cvbio.testing.UnitSpec
import io.cvbio.commons.ConfigurationUtil._
import io.cvbio.commons.CommonsDef._

import scala.util.Properties.isMac

class ConfigurationUtilTest extends UnitSpec {

  /** Whether Java is available or not? */
  lazy val bashAvailable: Boolean = new ProcessBuilder("bash", "-version").start().waitFor() == 0

  /** Whether Java is available or not? */
  lazy val javaAvailable: Boolean = new ProcessBuilder("java", "-version").start().waitFor() == 0

  "ConfigurationUtil.findExecutableInPath" should "return executable paths for common executables" in {
    if (bashAvailable) ConfigurationUtil.findExecutableInPath("bash") should not be empty
    if (javaAvailable) ConfigurationUtil.findExecutableInPath("java") should not be empty
  }

  "CommonsDef.findMacApplication" should "find the Safari internet browser" in {
    val garageBand: PathToMacApp = MacApplicationRoot.resolve( "Safari" + MacAppExtension)
    if (isMac) ConfigurationUtil.findMacApplication("Safari") shouldBe Some(garageBand)
  }

  "ConfigurationUtil.runAtShutdown" should "Schedule a thread to run a shutdown" in {
    // Not easy to test that this will be run at shutdown, so this exists to ensure no regressions in the API.
    ConfigurationUtil.runAtShutdown(() => 2 + 2)
  }
}
