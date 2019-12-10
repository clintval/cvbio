package io.cvbio.tools.util

import java.nio.file.Path

import com.fulcrumgenomics.commons.util.CaptureSystemStreams
import io.cvbio.commons.effectful.Io
import io.cvbio.testing.UnitSpec

class RelabelReferenceNamesTest extends UnitSpec with CaptureSystemStreams {

  /** Default delimiter for tests. */
  val Delimiter: Char = '\t'

  /** A chromosome name mapping. */
  val Hg38UcscToEnsemblAutosomes: Map[String, String] = (1 to 23).map { n => (s"chr$n", n.toString) }.toMap

  /** A chromosome name mapping as tab-delimited lines. */
  val MappingLines: Iterable[String] = Hg38UcscToEnsemblAutosomes.map { case (l, r) => Seq(l, r).mkString(Delimiter.toString) }

  /** A chromosome name mapping as tab-delimited lines in a file. */
  val MappingFile: Path = {
    val file = tempFile()
    Io.writeLines(file, MappingLines)
    file
  }

  "RelabelReferenceNames.relabel" should "successfully relabel a valid column position" in {
    val line     = Seq("chr1", "200030", "400000", "interval-name").mkString(Delimiter.toString)
    val expected = Seq("1", "200030", "400000", "interval-name").mkString(Delimiter.toString)
    val actual   = RelabelReferenceNames.relabel(line, Seq(0), Delimiter, Hg38UcscToEnsemblAutosomes)
    actual shouldBe expected
  }

  it should "successfully relabel multiple valid column positions" in {
    val line     = Seq("chr1", "chr2", "chr3", "chr4").mkString(Delimiter.toString)
    val expected = Seq("1", "2", "3", "chr4").mkString(Delimiter.toString)
    val actual   = RelabelReferenceNames.relabel(line, Seq(0, 1, 2), Delimiter, Hg38UcscToEnsemblAutosomes)
    actual shouldBe expected
  }

  it should "raise an exception by default if a lookup function fails" in {
    val line = Seq("chr29", "200030", "400000", "interval-name").mkString(Delimiter.toString)
    an[NoSuchElementException] shouldBe thrownBy { RelabelReferenceNames.relabel(line, Seq(0), Delimiter, Hg38UcscToEnsemblAutosomes) }
  }

  "RelabelReferenceNames.buildMapping" should "read a mapping from an iterator of lines" in {
    RelabelReferenceNames.buildMapping(MappingLines.iterator, Delimiter) shouldBe Hg38UcscToEnsemblAutosomes
  }

  it should "read a mapping from two-column delimited file" in {
    RelabelReferenceNames.buildMapping(MappingFile, Delimiter) shouldBe Hg38UcscToEnsemblAutosomes
  }

  "RelabelReferenceNames" should "relabel a text file" in {
    val original = Seq(Seq("chr1", "2", "3"), Seq("chr4", "4", "5"))
    val expected = Seq(Seq("1", "2", "3"), Seq("4", "4", "5"))

    val infile   = tempFile()
    val outfile  = tempFile()
    Io.writeLines(infile, original.map(_.mkString(Delimiter.toString)))

    val relabel = new RelabelReferenceNames(infile, outfile, MappingFile, columns = Seq(0), delimiter = Delimiter)
    relabel.execute()

    val actual = Io.readLines(outfile).map(_.split(Delimiter)).toList
    actual should contain theSameElementsInOrderAs expected
  }

  it should "raise an exception by default if a lookup function fails and drop is false" in {
    val original = Seq(Seq("chr1", "2", "3"), Seq("chr4", "4", "5"), Seq("illegal", "5", "6"))

    val infile   = tempFile()
    val outfile  = tempFile()
    Io.writeLines(infile, original.map(_.mkString(Delimiter.toString)))

    val relabel = new RelabelReferenceNames(infile, outfile, MappingFile, columns = Seq(0), delimiter = Delimiter, drop = false)
    a[NoSuchElementException] shouldBe thrownBy { relabel.execute() }
  }

  it should "drop any records with items not in the mapping when drop is true" in {
    val original = Seq(Seq("chr1", "2", "3"), Seq("chr4", "4", "5"), Seq("illegal", "5", "6"))
    val expected = Seq(Seq("1", "2", "3"), Seq("4", "4", "5"))

    val infile   = tempFile()
    val outfile  = tempFile()
    Io.writeLines(infile, original.map(_.mkString(Delimiter.toString)))

    val relabel = new RelabelReferenceNames(infile, outfile, MappingFile, columns = Seq(0), delimiter = Delimiter, drop = true)
    noException shouldBe thrownBy { captureLogger { () => relabel.execute() } }

    val actual = Io.readLines(outfile).map(_.split(Delimiter)).toList
    actual should contain theSameElementsInOrderAs expected
  }

  it should "drop any records with items not in the mapping when drop is true, but not lines starting with a skip prefix" in {
    val original = Seq(Seq("chr1", "2", "3"), Seq("# comment"), Seq("chr4", "4", "5"), Seq("illegal", "5", "6"))
    val expected = Seq(Seq("1", "2", "3"), Seq("# comment"), Seq("4", "4", "5"))

    val infile   = tempFile()
    val outfile  = tempFile()
    Io.writeLines(infile, original.map(_.mkString(Delimiter.toString)))

    val relabel = new RelabelReferenceNames(infile, outfile, MappingFile, columns = Seq(0), delimiter = Delimiter, drop = true)
    noException shouldBe thrownBy { captureLogger { () => relabel.execute() } }

    val actual = Io.readLines(outfile).map(_.split(Delimiter)).toList
    actual should contain theSameElementsInOrderAs expected
  }

  it should "write-out records with skip characters directly to the output file" in {
    val skipPrefixes = Seq("#", "track")

    val original = Seq(Seq("chr1", "2", "3"), Seq("# this is a comment"), Seq("track: this is a track"), Seq("chr4", "4", "5"))
    val expected = Seq(Seq("1", "2", "3"), Seq("# this is a comment"), Seq("track: this is a track"), Seq("4", "4", "5"))

    val infile   = tempFile()
    val outfile  = tempFile()
    Io.writeLines(infile, original.map(_.mkString(Delimiter.toString)))

    val relabel = new RelabelReferenceNames(infile, outfile, MappingFile, columns = Seq(0), delimiter = Delimiter, skipPrefixes = skipPrefixes)
    relabel.execute()

    val actual = Io.readLines(outfile).map(_.split(Delimiter)).toList
    actual should contain theSameElementsInOrderAs expected
  }
}
