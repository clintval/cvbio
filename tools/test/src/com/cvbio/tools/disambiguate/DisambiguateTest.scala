package com.cvbio.tools.disambiguate

import com.cvbio.testing.UnitSpec
import com.cvbio.tools.disambiguate.Disambiguate.DisambiguationStrategy._
import com.cvbio.tools.disambiguate.Disambiguate.firstAssemblyName
import com.fulcrumgenomics.bam.Template
import com.fulcrumgenomics.bam.api.{SamOrder, SamSource}
import com.fulcrumgenomics.testing.SamBuilder
import htsjdk.samtools.SAMTag.{AS, NM}
import htsjdk.samtools.{SAMSequenceDictionary => SamSequenceDictionary, SAMSequenceRecord => SamSequenceRecord}

class DisambiguateTest extends UnitSpec {

  /** Build a [[com.fulcrumgenomics.bam.api.SamSource]] that has a sequencer header with the defined assembly names. */
  private def sourceWith(names: Seq[String]): SamSource = {
    import com.fulcrumgenomics.commons.CommonsDef.IteratorToJavaCollectionsAdapter

    val records = names
      .zipWithIndex
      .map { case (name, index) =>
        val record = new SamSequenceRecord( s"contig$index", 100)
        record.setAssembly(name)
        record
      }
      .toIterator
      .toJavaList

    new SamBuilder(sd = Some(new SamSequenceDictionary(records))).toSource
  }

  "Disambiguate.firstAssemblyName" should "return a collection of the first sequence record assembly names" in {
    Seq(
      Seq("hs38DH"),
      Seq("mm10", "hs38DH"),
      Seq("rn6", null, "hs38DH")
    ).foreach { names => firstAssemblyName(sourceWith(names)).value shouldBe names.head }

    firstAssemblyName(sourceWith(Seq(null))) shouldBe None
  }

  "DisambiguationStrategy.ClassicDisambiguationStrategy" should "pick None when there is nothing to pick from" in {
    ClassicDisambiguationStrategy.choose(Seq.empty) shouldBe empty
  }

  it should "pick None when all alignments are unmapped" in {
    val builder = new SamBuilder(sort = Some(SamOrder.Queryname))
    val pair1   = builder.addPair(name = "pair1", unmapped1 = true, unmapped2 = true)
    val pair2   = builder.addPair(name = "pair2", unmapped1 = true, unmapped2 = true)

    val templates = Seq(pair1, pair2).map(pair => Template(pair.toIterator))

    ClassicDisambiguationStrategy.choose(templates) shouldBe None
  }

  it should "pick None if there are multiple of the same highest minimum and maximum alignment scores" in {
    val builder = new SamBuilder(sort = Some(SamOrder.Queryname))
    val pair1   = builder.addPair(name = "pair1", attrs = Map(AS.toString -> 50))
    val pair2   = builder.addPair(name = "pair2", attrs = Map(AS.toString -> 50))
    val pair3   = builder.addPair(name = "pair3", attrs = Map(AS.toString -> 50))

    val templates = Seq(pair1, pair2, pair3).map(pair => Template(pair.toIterator))

    ClassicDisambiguationStrategy.choose(templates) shouldBe None
  }

  it should "pick the template with the highest maximum alignment score" in {
    val builder = new SamBuilder(sort = Some(SamOrder.Queryname))
    val pair1   = builder.addPair(name = "pair1", attrs = Map(AS.toString -> 22))
    val pair2   = builder.addPair(name = "pair2", attrs = Map(AS.toString -> 22))
    val pair3   = builder.addPair(name = "pair3", attrs = Map(AS.toString -> 50))

    val templates = Seq(pair1, pair2, pair3).map(pair => Template(pair.toIterator))

    ClassicDisambiguationStrategy.choose(templates).value.name shouldBe "pair3"
  }

  it should "pick the template with the highest minimum alignment score when multiple maximums exist" in {
    val builder = new SamBuilder(sort = Some(SamOrder.Queryname))
    val pair1   = builder.addPair(name = "pair1", attrs = Map(AS.toString -> 22))
    val pair2   = builder.addPair(name = "pair2", attrs = Map(AS.toString -> 22))
    val pair3   = builder.addPair(name = "pair3", attrs = Map(AS.toString -> 16))

    pair1.take(1).foreach(rec => { rec(AS.toString) = 4 })
    pair2.take(1).foreach(rec => { rec(AS.toString) = 5 })
    pair3.take(1).foreach(rec => { rec(AS.toString) = 19 })

    val templates = Seq(pair1, pair2, pair3).map(pair => Template(pair.toIterator))

    ClassicDisambiguationStrategy.choose(templates).value.name shouldBe "pair3"
  }

  it should "pick None if there are multiple of the same lowest minimum and maximum alignment edit distances" in {
    val builder = new SamBuilder(sort = Some(SamOrder.Queryname))
    val pair1   = builder.addPair(name = "pair1", attrs = Map(NM.toString -> 50))
    val pair2   = builder.addPair(name = "pair2", attrs = Map(NM.toString -> 50))
    val pair3   = builder.addPair(name = "pair3", attrs = Map(NM.toString -> 50))

    val templates = Seq(pair1, pair2, pair3).map(pair => Template(pair.toIterator))

    ClassicDisambiguationStrategy.choose(templates) shouldBe None
  }

  it should "pick the template with the lowest minimum alignment edit distance" in {
    val builder = new SamBuilder(sort = Some(SamOrder.Queryname))
    val pair1   = builder.addPair(name = "pair1", attrs = Map(NM.toString -> 7))
    val pair2   = builder.addPair(name = "pair2", attrs = Map(NM.toString -> 6))
    val pair3   = builder.addPair(name = "pair3", attrs = Map(NM.toString -> 8))

    val templates = Seq(pair1, pair2, pair3).map(pair => Template(pair.toIterator))

    ClassicDisambiguationStrategy.choose(templates).value.name shouldBe "pair2"
  }

  it should "pick the template with the lowest maximum alignment edit distance when multiple minimums exist" in {
    val builder = new SamBuilder(sort = Some(SamOrder.Queryname))
    val pair1   = builder.addPair(name = "pair1", attrs = Map(NM.toString -> 6))
    val pair2   = builder.addPair(name = "pair2", attrs = Map(NM.toString -> 6))
    val pair3   = builder.addPair(name = "pair3", attrs = Map(NM.toString -> 6))

    pair1.take(1).foreach(rec => { rec(NM.toString) = 75 })
    pair2.take(1).foreach(rec => { rec(NM.toString) = 50 })
    pair3.take(1).foreach(rec => { rec(NM.toString) = 25 })

    val templates = Seq(pair1, pair2, pair3).map(pair => Template(pair.toIterator))

    ClassicDisambiguationStrategy.choose(templates).value.name shouldBe "pair3"
  }
}
