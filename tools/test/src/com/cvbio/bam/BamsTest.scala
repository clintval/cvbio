package com.cvbio.bam

import com.cvbio.testing.UnitSpec
import com.fulcrumgenomics.bam.api.{SamOrder, SamSource}
import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.testing.SamBuilder

class BamsTest extends UnitSpec {

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
