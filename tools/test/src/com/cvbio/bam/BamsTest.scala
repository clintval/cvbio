package com.cvbio.bam

import com.cvbio.bam.Bams.ReadOrdinal.{All, Read1, Read2}
import com.cvbio.testing.{TemplateBuilder, UnitSpec}
import com.fulcrumgenomics.bam.api.{SamOrder, SamSource}
import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.testing.SamBuilder

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
    val caught  = intercept[AssertionError] { Bams.templatesIterator(sources: _*).toList }
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

  "Bams.sortedTemplateIterator" should "always return a query sorted SAM record iterator" in {
    val builder1 = new SamBuilder(sort = Some(SamOrder.Unknown))
    builder1.addPair(name = "A9")
    builder1.addPair(name = "A88")
    val iterator1 = Bams.querySortedIterator(builder1.iterator, builder1.header, maxInMemory = 2, Bams.DefaultSortingTempDirectory)
    iterator1.toList.map(_.name) should contain theSameElementsInOrderAs Seq("A88", "A88", "A9", "A9")

    val builder2 = new SamBuilder(sort = Some(SamOrder.Unknown))
    builder2.addPair(name = "A88")
    builder2.addPair(name = "A9")
    val iterator2 = Bams.querySortedIterator(builder2.iterator, builder2.header, maxInMemory = 2, Bams.DefaultSortingTempDirectory)
    iterator2.toList.map(_.name) should contain theSameElementsInOrderAs Seq("A88", "A88", "A9", "A9")
  }

  "Bams.sortedTemplateIterator" should "only return templates in a queryname sorted order" in {
    val builder1 = new SamBuilder(sort = Some(SamOrder.Unknown))
    builder1.addPair(name = "A9")
    builder1.addPair(name = "A88")
    val iterator1 = Bams.sortedTemplateIterator(builder1.iterator, builder1.header, maxInMemory = 2, Bams.DefaultSortingTempDirectory)
    iterator1.toList.map(_.name) should contain theSameElementsInOrderAs Seq("A88", "A9")

    val builder2 = new SamBuilder(sort = Some(SamOrder.Unknown))
    builder2.addPair(name = "A88")
    builder2.addPair(name = "A9")
    val iterator2 = Bams.sortedTemplateIterator(builder2.iterator, builder2.header, maxInMemory = 2, Bams.DefaultSortingTempDirectory)
    iterator2.toList.map(_.name) should contain theSameElementsInOrderAs Seq("A88", "A9")
  }
}
