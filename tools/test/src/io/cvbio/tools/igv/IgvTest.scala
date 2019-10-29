package io.cvbio.tools.igv

import io.cvbio.testing.UnitSpec
import io.cvbio.tools.igv.Igv.OutputFormat

class IgvTest extends UnitSpec {

  "Igv" should "fail to launch with a bogus host and port" in {
    an[IllegalArgumentException] should be thrownBy { new Igv("a-nothing-host", 200) }
  }

  "Igv.available" should "return false for a bogus host and port" in {
    Igv.available("a-nothing-host", 2000000) shouldBe false
  }

  "OutputFormat" should "have the correct extensions" in {
    OutputFormat.Jpg.suffix shouldBe ".jpg"
    OutputFormat.Png.suffix shouldBe ".png"
    OutputFormat.Svg.suffix shouldBe ".svg"
  }
}
