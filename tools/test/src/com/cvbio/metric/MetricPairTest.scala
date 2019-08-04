package com.cvbio.metric

import com.cvbio.testing.UnitSpec

class MetricPairTest extends UnitSpec {

  "MetricPair" should "be equivalent when empty" in {
    MetricPair.empty[Int].compare(MetricPair.empty[Int]) shouldBe 0
    MetricPair.empty[Int].compareTo(MetricPair.empty[Int]) shouldBe 0

    MetricPair.empty[String].compare(MetricPair.empty[String]) shouldBe 0
    MetricPair.empty[Double].compare(MetricPair.empty[Double]) shouldBe 0
  }

  it should "by default, prefer ordering maximum values first when only one read is defined" in {
    val test1 = new MetricPair[Int](Some(2), None)
    val test2 = new MetricPair[Int](Some(1), None)
    test1.compare(test1) shouldBe 0
    test2.compare(test2) shouldBe 0
    test1.compare(test2) shouldBe 1
    test2.compare(test1) shouldBe -1
  }

  it should "by default, prefer ordering maximum values first 2 / 4 reads are defined" in {
    val test1 = new MetricPair[Int](None, Some(2))
    val test2 = new MetricPair[Int](Some(1), None)
    test1.compare(test1) shouldBe 0
    test2.compare(test2) shouldBe 0
    test1.compare(test2) shouldBe 1
    test2.compare(test1) shouldBe -1
  }

  it should "by default, prefer ordering maximum values first when 3 / 4 reads are defined" in {
    val test1 = new MetricPair[Int](Some(1), Some(2))
    val test2 = new MetricPair[Int](Some(3), None)
    test2.compare(test2) shouldBe 0
    test1.compare(test1) shouldBe 0
    test1.compare(test2) shouldBe -1
    test2.compare(test1) shouldBe 1
  }

  it should "by default, prefer ordering maximum values first when 4 / 4 reads are defined" in {
    val test1 = new MetricPair[Int](Some(1), Some(2))
    val test2 = new MetricPair[Int](Some(3), Some(3))
    test2.compare(test2) shouldBe 0
    test1.compare(test1) shouldBe 0
    test1.compare(test2) shouldBe -1
    test2.compare(test1) shouldBe 1
  }

  it should "by default, prefer ordering minimum values if maximum values are equivalent" in {
    val test1 = new MetricPair[Int](Some(10), Some(2))
    val test2 = new MetricPair[Int](Some(3), Some(10))
    test2.compare(test2) shouldBe 0
    test1.compare(test1) shouldBe 0
    test1.compare(test2) shouldBe -1
    test2.compare(test1) shouldBe 1
  }

  it should "support equality operators" in {
    val test1 = new MetricPair[Int](Some(10), Some(2))
    val test2 = new MetricPair[Int](Some(3), Some(10))
    test1 >  test2 shouldBe false
    test1 >= test2 shouldBe false
    test1 <  test2 shouldBe true
    test1 <= test2 shouldBe true
    val test3 = new MetricPair[Int](Some(10), Some(3))
    val test4 = new MetricPair[Int](Some(3), Some(10))
    test3 >= test4
    test4 >= test3
  }
}
