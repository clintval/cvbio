package com.cvbio.commons.io

import com.cvbio.testing.UnitSpec
import com.cvbio.commons.io.Io.PathUtil
import com.fulcrumgenomics.commons.io.PathUtil.{basename, pathTo}

class IoTest extends UnitSpec {

  "Io.PathUtil" should "provide a better DSL for appending strings to paths and resolving paths" in {
    val path = pathTo("test") / "hi!"
    basename(path) shouldBe "hi!"
    basename(path.getParent) shouldBe "test"
    basename(path + "-Mom!") shouldBe "hi!-Mom!"
  }
}
