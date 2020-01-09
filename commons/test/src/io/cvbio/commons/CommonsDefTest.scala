package io.cvbio.commons

import io.cvbio.commons.CommonsDef._
import io.cvbio.testing.UnitSpec
import com.fulcrumgenomics.commons.io.PathUtil

import scala.collection.mutable.ListBuffer

class CommonsDefTest extends UnitSpec {

  "CommonsDef.{bai, bamBai, crai, cramCrai}" should "build the correct paths for HTS sequencing data indexes" in {
    bai(PathUtil.pathTo("test.bam"))       shouldBe PathUtil.pathTo("test.bai")
    bamBai(PathUtil.pathTo("test.bam"))    shouldBe PathUtil.pathTo("test.bam.bai")
    crai(PathUtil.pathTo("test.cram"))     shouldBe PathUtil.pathTo("test.crai")
    cramCrai(PathUtil.pathTo("test.cram")) shouldBe PathUtil.pathTo("test.cram.crai")
  }

  "CommonsDef.tapEach" should "execute a function but return it's identity" in {
    val coll           = Seq(1, 2, 3)
    val sideEffectSink = new ListBuffer[Int]()
    val tapped         = coll.tapEach(sideEffectSink.append(_)).toList // .toList forces the stream

    coll should contain theSameElementsInOrderAs sideEffectSink
    coll should contain theSameElementsInOrderAs tapped
  }

  "CommonsDef.interleave" should "interleave an item after every other item" in {
    val coll: Seq[String] = Seq("1", "2", "3")
    interleave(sep = "\n")(coll).mkString shouldBe (coll.mkString("\n") + "\n")
  }

  "CommonsDef.patchManyWith" should "patch in known items within a collection using a simple function" in {
    val original = Seq(1, 2 ,3, 4)
    val expected = Seq(4, 4, 3, 4)
    val actual   = patchManyWith[Int](original, Seq(0, 1), _ => 4)
    actual shouldBe expected
  }

  "CommonsDef.patchManyWith" should "patch in known items within a collection using a lookup function" in {
    val original = Seq(1, 2 ,3, 4)
    val expected = Seq(2, 3, 4, 5)
    val lookup   = Map[Int, Int](1 -> 2, 2 -> 3, 3 -> 4, 4 -> 5)
    val actual   = patchManyWith[Int](original, Seq(0, 1, 2, 3), lookup)
    actual shouldBe expected
  }

  "CommonsDef.patchManyWith" should "ignore indexes that don't actually exist" in {
    val original = Seq(1, 2 ,3, 4)
    val actual   = patchManyWith[Int](original, Seq(-1, 4), _ => 4)
    actual shouldBe original
  }
}
