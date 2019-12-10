package io.cvbio.tools.util

import java.nio.file.Path

import com.fulcrumgenomics.commons.util.CaptureSystemStreams
import io.cvbio.commons.effectful.Io
import io.cvbio.testing.UnitSpec

class UpdateDataContigNamesTest extends UnitSpec with CaptureSystemStreams {

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

  "UpdateDataContigNames.update" should "successfully update a valid column position" in {
    val line     = Seq("chr1", "200030", "400000", "interval-name").mkString(Delimiter.toString)
    val expected = Seq("1", "200030", "400000", "interval-name").mkString(Delimiter.toString)
    val actual   = UpdateDataContigNames.update(line, Seq(0), Delimiter, Hg38UcscToEnsemblAutosomes)
    actual shouldBe expected
  }

  it should "successfully update multiple valid column positions" in {
    val line     = Seq("chr1", "chr2", "chr3", "chr4").mkString(Delimiter.toString)
    val expected = Seq("1", "2", "3", "chr4").mkString(Delimiter.toString)
    val actual   = UpdateDataContigNames.update(line, Seq(0, 1, 2), Delimiter, Hg38UcscToEnsemblAutosomes)
    actual shouldBe expected
  }

  it should "raise an exception by default if a lookup function fails" in {
    val line = Seq("chr29", "200030", "400000", "interval-name").mkString(Delimiter.toString)
    an[NoSuchElementException] shouldBe thrownBy { UpdateDataContigNames.update(line, Seq(0), Delimiter, Hg38UcscToEnsemblAutosomes) }
  }

  "UpdateDataContigNames.buildMapping" should "read a mapping from an iterator of lines" in {
    UpdateDataContigNames.buildMapping(MappingLines.iterator, Delimiter) shouldBe Hg38UcscToEnsemblAutosomes
  }

  it should "read a mapping from two-column delimited file" in {
    UpdateDataContigNames.buildMapping(MappingFile, Delimiter) shouldBe Hg38UcscToEnsemblAutosomes
  }

  "UpdateDataContigNames" should "update a text file" in {
    val original = Seq(Seq("chr1", "2", "3"), Seq("chr4", "4", "5"))
    val expected = Seq(Seq("1", "2", "3"), Seq("4", "4", "5"))

    val infile   = tempFile()
    val outfile  = tempFile()
    Io.writeLines(infile, original.map(_.mkString(Delimiter.toString)))

    val update = new UpdateDataContigNames(infile, outfile, mapping = MappingFile, columns = Seq(0), delimiter = Delimiter)
    update.execute()

    val actual = Io.readLines(outfile).map(_.split(Delimiter)).toList
    actual should contain theSameElementsInOrderAs expected
  }

  it should "raise an exception by default if a lookup function fails and drop is false" in {
    val original = Seq(Seq("chr1", "2", "3"), Seq("chr4", "4", "5"), Seq("illegal", "5", "6"))

    val infile   = tempFile()
    val outfile  = tempFile()
    Io.writeLines(infile, original.map(_.mkString(Delimiter.toString)))

    val update = new UpdateDataContigNames(infile, outfile, mapping = MappingFile, columns = Seq(0), delimiter = Delimiter, skipMissing = false)
    a[NoSuchElementException] shouldBe thrownBy { update.execute() }
  }

  it should "drop any records with items not in the mapping when drop is true" in {
    val original = Seq(Seq("chr1", "2", "3"), Seq("chr4", "4", "5"), Seq("illegal", "5", "6"))
    val expected = Seq(Seq("1", "2", "3"), Seq("4", "4", "5"))

    val infile   = tempFile()
    val outfile  = tempFile()
    Io.writeLines(infile, original.map(_.mkString(Delimiter.toString)))

    val update = new UpdateDataContigNames(infile, outfile, mapping = MappingFile, columns = Seq(0), delimiter = Delimiter, skipMissing = true)
    noException shouldBe thrownBy { captureLogger { () => update.execute() } }

    val actual = Io.readLines(outfile).map(_.split(Delimiter)).toList
    actual should contain theSameElementsInOrderAs expected
  }

  it should "drop any records with items not in the mapping when skip missing is true, but not lines starting with a skip prefix" in {
    val original = Seq(Seq("chr1", "2", "3"), Seq("# comment"), Seq("chr4", "4", "5"), Seq("illegal", "5", "6"))
    val expected = Seq(Seq("1", "2", "3"), Seq("# comment"), Seq("4", "4", "5"))

    val infile   = tempFile()
    val outfile  = tempFile()
    Io.writeLines(infile, original.map(_.mkString(Delimiter.toString)))

    val update = new UpdateDataContigNames(infile, outfile, mapping = MappingFile, columns = Seq(0), delimiter = Delimiter, skipMissing = true)
    noException shouldBe thrownBy { captureLogger { () => update.execute() } }

    val actual = Io.readLines(outfile).map(_.split(Delimiter)).toList
    actual should contain theSameElementsInOrderAs expected
  }

  it should "write-out records with comment characters as prefixes, directly to the output file" in {
    val skipPrefixes = Seq("#", "track")

    val original = Seq(Seq("chr1", "2", "3"), Seq("# this is a comment"), Seq("track: this is a track"), Seq("chr4", "4", "5"))
    val expected = Seq(Seq("1", "2", "3"), Seq("# this is a comment"), Seq("track: this is a track"), Seq("4", "4", "5"))

    val infile   = tempFile()
    val outfile  = tempFile()
    Io.writeLines(infile, original.map(_.mkString(Delimiter.toString)))

    val update = new UpdateDataContigNames(infile, outfile, mapping = MappingFile, columns = Seq(0), delimiter = Delimiter, commentChars = skipPrefixes)
    update.execute()

    val actual = Io.readLines(outfile).map(_.split(Delimiter)).toList
    actual should contain theSameElementsInOrderAs expected
  }
}
