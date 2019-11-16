package io.cvbio.commons

import io.cvbio.testing.UnitSpec

class StringUtilTest extends UnitSpec {

  "StringUtil.uncapitilize" should "do nothing on an empty string" in {
    StringUtil.uncapitilize("") shouldBe ""
  }

  it should "uncapitilize a single letter only when only one is present" in {
    StringUtil.uncapitilize("a") shouldBe "a"
    StringUtil.uncapitilize("A") shouldBe "a"
    StringUtil.uncapitilize("0") shouldBe "0"
    StringUtil.uncapitilize("!") shouldBe "!"
  }

  it should "uncapitilize the first of many characters" in {
    StringUtil.uncapitilize("obama") shouldBe "obama"
    StringUtil.uncapitilize("Obama") shouldBe "obama"
    StringUtil.uncapitilize("OBAMA") shouldBe "oBAMA"
    StringUtil.uncapitilize("D####") shouldBe "d####"
  }
}
