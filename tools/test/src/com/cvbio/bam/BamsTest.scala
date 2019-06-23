package com.cvbio.bam

import com.cvbio.bam.Bams.TemplateUtil
import com.cvbio.testing.UnitSpec
import com.fulcrumgenomics.bam.Template
import com.fulcrumgenomics.bam.api.{SamOrder, SamSource}
import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.testing.SamBuilder

class BamsTest extends UnitSpec {

  "Bams.TemplateUtil" should "return every template's read1 or read2 in a single collection" in {
    val builder = new SamBuilder(sort = Some(SamOrder.Queryname))
    val pair1   = builder.addPair(name = "pair1")
    val pair2   = builder.addPair(name = "pair2")
    val pair3   = builder.addPair(name = "pair3")

    val pair2Secondary     = builder.addPair(name = "pair2")
    val pair2Supplementary = builder.addPair(name = "pair2")

    pair2Secondary.foreach(_.secondary = true)
    pair2Supplementary.foreach(_.supplementary = true)

    val templates = Seq(
      Template(pair1.toIterator),
      Template((pair2 ++ pair2Secondary ++ pair2Supplementary).toIterator),
      Template(pair3.toIterator)
    )

    val expected = Seq(pair1, pair2, pair2Secondary, pair2Supplementary, pair3)

    templates.flatMap(_.allR1) should contain theSameElementsInOrderAs expected.flatMap(_.take(1))
    templates.flatMap(_.allR2) should contain theSameElementsInOrderAs expected.flatMap(_.takeRight(1))
  }

  "Bams.querynameSorted" should "only return true if the header is set to a queryname sort" in {
    SamOrder.values.foreach { so =>
      Bams.querynameSorted(new SamBuilder(sort = Some(so)).toSource) shouldBe so == SamOrder.Queryname
    }
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
    iterator.length shouldBe 1
  }

  it should "require every SamSource to be queryname sorted" in {
    val source1 = new SamBuilder(sort = Some(SamOrder.Queryname)).toSource
    val source2 = new SamBuilder(sort = Some(SamOrder.Queryname)).toSource
    noException should be thrownBy Bams.templatesIterator(source1, source2)

    SamOrder.values.filterNot(_ == SamOrder.Queryname).foreach { so =>
      val source1 = new SamBuilder(sort = Some(so)).toSource
      val source2 = new SamBuilder(sort = Some(so)).toSource
      assertThrows[IllegalArgumentException] { Bams.templatesIterator(source1, source2) }
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
    caught.getMessage should include("Templates do not have the same name:")
  }

  it should "require that all underlying SamSources have the same number of ordered templates" in {
    val builder1 = new SamBuilder(sort = Some(SamOrder.Queryname))
    val builder2 = new SamBuilder(sort = Some(SamOrder.Queryname))

    builder1.addPair(name = "pair1", start1 = 1, start2 = 2)
    builder1.addPair(name = "pair2", start1 = 1, start2 = 2)
    builder2.addPair(name = "pair1", start1 = 1, start2 = 2)

    val caught = intercept[IllegalArgumentException] { Bams.templatesIterator(builder1.toSource, builder2.toSource).toList }
    caught.getMessage should include("SAM sources do not have the same number of templates")
  }
}
