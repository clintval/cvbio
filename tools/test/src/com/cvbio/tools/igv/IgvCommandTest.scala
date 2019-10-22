package com.cvbio.tools.igv

import com.cvbio.testing.UnitSpec

class IgvCommandTest extends UnitSpec {

  "IgvCommand" should "use the class name as the simple name" in {
    Echo.toString   shouldBe "echo"
    Echo.simpleName shouldBe "echo"
    Exit.toString   shouldBe "exit"
    Exit.simpleName shouldBe "exit"
    New.toString    shouldBe "new"
    New.simpleName  shouldBe "new"
  }
}