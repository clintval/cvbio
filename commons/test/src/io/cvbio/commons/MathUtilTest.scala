package io.cvbio.commons

import io.cvbio.testing.UnitSpec

class MathUtilTest extends UnitSpec {

  "MathUtil.WithCountMaxMinBy" should "transform a sequence and pick the maximum or minimum item" in {
    import io.cvbio.commons.MathUtil.WithCountMaxMinBy

    Seq[Int]().countMaxBy(identity) shouldBe 0
    Seq[Int]().countMinBy(identity) shouldBe 0
    Seq("1", "1", "2", "3", "3", "4").countMaxBy(identity) shouldBe 1
    Seq("1", "1", "2", "3", "3", "4").countMinBy(identity) shouldBe 2
    Seq("1", "1", "2", "3", "3", "4").countMaxBy(_.toInt)  shouldBe 1
    Seq("1", "1", "2", "3", "3", "4").countMinBy(_.toInt)  shouldBe 2
    Seq(-1, 22, 333, 333, 444, 444).countMaxBy(identity)   shouldBe 2
    Seq(-1, 22, 333, 333, 444, 444).countMinBy(identity)   shouldBe 1
  }

  "MathUtil.WithMaxMinOption" should "find the maximum or minimum item in a sequence if it exists" in {
    import io.cvbio.commons.MathUtil.WithMaxMinOption

    Seq[Int]().minOption shouldBe None
    Seq[Int]().maxOption shouldBe None
    Seq(-1, 22, 333, 333, 444, 444).minOption.value shouldBe -1
    Seq(-1, 22, 333, 333, 444, 444).maxOption.value shouldBe 444
  }

  "MathUtil.WithPickMaxMinBy" should "transform a sequence and pick the single maximum or minimum item, if it exists" in {
    import io.cvbio.commons.MathUtil.WithPickMaxMinBy

    Seq[Int]().pickMaxBy(identity) shouldBe None
    Seq[Int]().pickMinBy(identity) shouldBe None
    Seq("1", "1", "2", "3", "3", "4").pickMaxBy(identity).value shouldBe "4"
    Seq("1", "1", "2", "3", "3", "4").pickMinBy(identity)       shouldBe None
    Seq("1", "1", "2", "3", "3", "4").pickMaxBy(_.toInt).value  shouldBe "4"
    Seq("1", "1", "2", "3", "3", "4").pickMinBy(_.toInt)        shouldBe None
    Seq(-1, 22, 333, 333, 444, 444).pickMaxBy(identity)         shouldBe None
    Seq(-1, 22, 333, 333, 444, 444).pickMinBy(identity).value   shouldBe -1
  }

  "MathUtil.{countMax, countMin}" should "return the count of the maximum and minimum items in a sequence" in {
    MathUtil.countMax(Seq[Int]()) shouldBe 0
    MathUtil.countMin(Seq[Int]()) shouldBe 0
    MathUtil.countMax(Seq(1, 1, 2, 3, 3, 4)) shouldBe 1
    MathUtil.countMin(Seq(1, 1, 2, 3, 3, 4)) shouldBe 2
    MathUtil.countMax(Seq(1, 22, 333, 444))  shouldBe 1
    MathUtil.countMin(Seq(1, 22, 333, 444))  shouldBe 1
  }

  "MathUtil.{pickMax, pickMin}" should "pick the single maximum or minimum item, if it exists" in {
    MathUtil.pickMax(Seq[Int]()) shouldBe None
    MathUtil.pickMin(Seq[Int]()) shouldBe None
    MathUtil.pickMax(Seq(1, 1, 2, 3, 3, 4)).value shouldBe 4
    MathUtil.pickMin(Seq(1, 1, 2, 3, 3, 4))       shouldBe None
    MathUtil.pickMax(Seq(1, 22, 333, 444)).value  shouldBe 444
    MathUtil.pickMin(Seq(1, 22, 333, 444)).value  shouldBe 1
  }
}
