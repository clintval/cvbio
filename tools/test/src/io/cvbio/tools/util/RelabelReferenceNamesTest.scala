package io.cvbio.tools.util

import io.cvbio.testing.UnitSpec

class RelabelReferenceNamesTest extends UnitSpec {

  private val delimiter: Char = '\t'

  val Hg38UcscToEnsemblAutosomes: Map[String, String] = (1 to 23).map { n => (s"chr$n", n.toString)}.toMap

  "RelabelReferenceNames.relabelLine" should "successfully relabel valid column positions" in {
    val line     = Seq("chr1", "200030", "400000", "interval-name").mkString(delimiter.toString)
    val expected = Seq("1", "200030", "400000", "interval-name").mkString(delimiter.toString)
    val actual   = RelabelReferenceNames.relabelLine(line, Hg38UcscToEnsemblAutosomes)
    actual shouldBe expected
  }

  "RelabelReferenceNames.relabelLine" should "raise an exception by default if a reference name does not exist" in {
    val line = Seq("chr29", "200030", "400000", "interval-name").mkString(delimiter.toString)
    an[IllegalArgumentException] shouldBe thrownBy { RelabelReferenceNames.relabelLine(line, Hg38UcscToEnsemblAutosomes) }
  }

  "RelabelReferenceNames.relabelLine" should "not raise an exception if a reference name does not exist if asked not to" in {
    val line = Seq("chr29", "200030", "400000", "interval-name").mkString(delimiter.toString)
    noException shouldBe thrownBy {
      line shouldBe RelabelReferenceNames.relabelLine(line, Hg38UcscToEnsemblAutosomes, requireExists = false)
    }
  }
}
