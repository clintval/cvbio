package com.cvbio.bam

import com.cvbio.bam.Bams._
import com.cvbio.bam.Bams.ReadOrdinal.{All, Read1, Read2}
import com.cvbio.testing.{TemplateBuilder, UnitSpec}
import com.fulcrumgenomics.bam.api.{SamOrder, SamSource}
import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.testing.SamBuilder
import htsjdk.samtools.SAMFileHeader.GroupOrder

class BamsTest extends UnitSpec {

  "Bams.TemplateUtil" should "provide easy to all read tags across a read ordinal" in {
    import com.cvbio.bam.Bams.TemplateUtil

    val builder = new TemplateBuilder(name = "test")

    builder.addPrimaryPair(r1Attrs = Map("NM" -> 2), r2Attrs = Map("NM" -> 3))
    builder.addSecondaryPair(r1Attrs = Map("NM" -> 6), r2Attrs = Map("NM" -> 10))
    builder.addSupplementaryPair(r1Attrs = Map("NM" -> null), r2Attrs = Map("NM" -> null))
    builder.addSupplementaryPair(r1Attrs = Map("NM" -> 16))

    builder.template.tagValues[Int](Read1, tag = "NM").flatten should contain theSameElementsInOrderAs Seq(2, 6, 16)
    builder.template.tagValues[Int](Read2, tag = "NM").flatten should contain theSameElementsInOrderAs Seq(3, 10)
    builder.template.tagValues[Int](All,   tag = "NM").flatten should contain theSameElementsInOrderAs Seq(2, 3, 16, 6, 10)
  }

  "Bams.templatesIterator" should "accept no SamSource and do nothing" in {
    val iterator = Bams.templatesIterator()
    iterator.hasNext shouldBe false
  }

  it should "accept a single valid SamSource with no alignments" in {
    val builder  = new SamBuilder(sort = Some(SamOrder.Queryname))
    val iterator = Bams.templatesIterator(builder.toSource)
    iterator.hasNext shouldBe false
  }

  it should "accept a single valid SamSource with more than zero alignments" in {
    val builder  = new SamBuilder(sort = Some(SamOrder.Queryname))
    builder.addPair(name = "pair1", start1 = 1, start2 = 2)
    val iterator = Bams.templatesIterator(builder.toSource)
    iterator.hasNext shouldBe true
    iterator.length  shouldBe 1
  }

  it should "not require every SamSource to be queryname sorted" in {
    val source1 = new SamBuilder(sort = Some(SamOrder.Queryname)).toSource
    val source2 = new SamBuilder(sort = Some(SamOrder.Queryname)).toSource

    SamOrder.values.foreach { so =>
      val source1 = new SamBuilder(sort = Some(so)).toSource
      val source2 = new SamBuilder(sort = Some(so)).toSource
      noException shouldBe thrownBy { Bams.templatesIterator(source1, source2) }
    }
  }

  it should "require that template names are actually synchronized" in {
    // NB: Just because a SAM file says it is in queryname sort does not mean that the sort order of the templates is
    //     actually stable since different tools and locales will produce different orderings. See the following:
    //     https://github.com/samtools/hts-specs/pull/361
    //     https://twitter.com/clint_valentine/status/1138875477974634496
    val tools   = Seq("picard", "samtools")
    val sources = tools.map(tool => SamSource(PathUtil.pathTo(first = s"tools/test/resources/$tool.queryname-sort.sam")))
    val caught  = intercept[IllegalArgumentException] { Bams.templatesIterator(sources: _*).toList }
    caught.getMessage should include("Templates with different names found!")
  }

  it should "require that all underlying SamSources have the same number of ordered templates" in {
    val builder1 = new SamBuilder(sort = Some(SamOrder.Queryname))
    val builder2 = new SamBuilder(sort = Some(SamOrder.Queryname))

    builder1.addPair(name = "pair1", start1 = 1, start2 = 2)
    builder1.addPair(name = "pair2", start1 = 1, start2 = 2)
    builder2.addPair(name = "pair1", start1 = 1, start2 = 2)

    val caught = intercept[AssertionError] { Bams.templatesIterator(builder1.toSource, builder2.toSource).toList }
    caught.getMessage should include("SAM sources do not have the same number of templates")
  }

  "Bams.querySortedIterator" should "work on a reader that is already query sorted" in {
    val builder = new SamBuilder(sort=Some(SamOrder.Queryname))
    builder.addFrag(name="q1", start=100)
    builder.addPair(name="p1", start1=100, start2=300)
    builder.addFrag(name="q2", start=200)

    Bams.querySortedIterator(in=builder.toSource).map(_.name).toSeq shouldBe Seq("p1", "p1", "q1", "q2")
  }

  it should "use the htsjdk sorting of querynames" in {
    // NB: We expect the queryname sort ordering of SamRecord objects to be stable and to reflect the
    //     default in both htsjdk and Picard. For more information, see the discussion at:
    //     https://github.com/samtools/hts-specs/pull/361#issuecomment-447065728
    val builder = new SamBuilder()

    val actual   = Seq("A1", "A2", "A7", "A9", "A10", "A12", "A88")
    val expected = Seq("A1", "A10", "A12", "A2", "A7", "A88", "A9")

    actual.foreach(name => builder.addFrag(name, start=100))
    Bams.querySortedIterator(in=builder.toSource).map(_.name).toSeq shouldBe expected
  }

  it should "sort a reader that is query grouped" in {
    val builder = new SamBuilder(sort=None)
    builder.header.setGroupOrder(GroupOrder.query)
    builder.addFrag(name="q1", start=100)
    builder.addPair(name="p1", start1=100, start2=300)
    builder.addFrag(name="q2", start=200)

    Bams.querySortedIterator(in=builder.toSource).map(_.name).toSeq shouldBe Seq("p1", "p1", "q1", "q2")
  }

  it should "sort a coordinate sorted input" in {
    val builder = new SamBuilder(sort=Some(SamOrder.Coordinate))
    builder.addFrag(name="q1", start=100)
    builder.addPair(name="p1", start1=100, start2=300)
    builder.addFrag(name="q2", start=200)

    Bams.querySortedIterator(in=builder.toSource).map(_.name).toSeq shouldBe Seq("p1", "p1", "q1", "q2")
  }

  it should "sort an unsorted input" in {
    val builder = new SamBuilder(sort=None)
    builder.addFrag(name="q1", start=100)
    builder.addPair(name="p1", start1=100, start2=300)
    builder.addFrag(name="q2", start=200)

    Bams.querySortedIterator(in=builder.toSource).map(_.name).toSeq shouldBe Seq("p1", "p1", "q1", "q2")
  }

  it should "accept a generic iterator as input" in {
    val builder = new SamBuilder(sort=None)
    builder.addFrag(name="q1", start=100)
    builder.addPair(name="p1", start1=100, start2=300)
    builder.addFrag(name="q2", start=200)

    val actual = Bams.querySortedIterator(builder.iterator, builder.header, maxInMemory = 10, DefaultSortingTempDirectory)
    actual.map(_.name) shouldBe Seq("p1", "p1", "q1", "q2")
  }

  "Bams.sortedTemplateIterator" should "return template objects in order" in {
    val builder = new SamBuilder(sort=Some(SamOrder.Coordinate))
    builder.addPair(name="p1", start1=100, start2=300)
    builder.addFrag(name="f1", start=100)
    builder.addPair(name="p2", start1=500, start2=200)
    builder.addPair(name="p0", start1=700, start2=999)

    val templates = Bams.templateSortedIterator(builder.toSource).toSeq
    templates should have size 4
    templates.map(_.name) shouldBe Seq("f1", "p0", "p1", "p2")

    templates.foreach {t =>
      (t.r1Supplementals ++ t.r1Secondaries ++ t.r2Supplementals ++ t.r2Secondaries).isEmpty shouldBe true
      if (t.name startsWith "f") {
        t.r1.exists(r => !r.paired) shouldBe true
        t.r2.isEmpty shouldBe true
      }
      else {
        t.r1.exists(r => r.firstOfPair) shouldBe true
        t.r2.exists(r => r.secondOfPair) shouldBe true
      }
    }
  }

  it should "only return templates in a queryname sorted order" in {
    val builder1 = new SamBuilder(sort = Some(SamOrder.Unknown))
    builder1.addPair(name = "A9")
    builder1.addPair(name = "A88")
    val iterator1 = Bams.templateSortedIterator(builder1.iterator, builder1.header, maxInMemory = 10, DefaultSortingTempDirectory)
    iterator1.toList.map(_.name) should contain theSameElementsInOrderAs Seq("A88", "A9")

    val builder2 = new SamBuilder(sort = Some(SamOrder.Unknown))
    builder2.addPair(name = "A88")
    builder2.addPair(name = "A9")
    val iterator2 = Bams.templateSortedIterator(builder2.iterator, builder2.header, maxInMemory = 10, DefaultSortingTempDirectory)
    iterator2.toList.map(_.name) should contain theSameElementsInOrderAs Seq("A88", "A9")
  }
}
