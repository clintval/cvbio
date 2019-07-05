package com.cvbio.commons

import com.cvbio.commons.CommonsDef._
import com.cvbio.testing.UnitSpec
import com.fulcrumgenomics.commons.io.PathUtil

class CommonsDefTest extends UnitSpec {

  "CommonsDef.{bai, bamBai, crai, cramCrai}" should "build the correct paths for HTS sequencing data indexes" in {
    bai(PathUtil.pathTo("test.bam"))       shouldBe PathUtil.pathTo("test.bai")
    bamBai(PathUtil.pathTo("test.bam"))    shouldBe PathUtil.pathTo("test.bam.bai")
    crai(PathUtil.pathTo("test.cram"))     shouldBe PathUtil.pathTo("test.crai")
    cramCrai(PathUtil.pathTo("test.cram")) shouldBe PathUtil.pathTo("test.cram.crai")
  }
}
