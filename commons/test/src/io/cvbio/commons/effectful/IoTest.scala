package io.cvbio.commons.effectful

import io.cvbio.testing.UnitSpec

class IoTest extends UnitSpec {

  "Io.readable" should "know when a file path is readable or not" in {
    Seq(true, false).map(condition => Io.readable(tempFile(readable = condition)) shouldBe condition)
  }

  "Io.writable" should "know when a file path is writable or not" in {
    Seq(true, false).map(condition => Io.writable(tempFile(writable = condition)) shouldBe condition)
  }

  "Io.toLineWriter" should "write complete lines to an output file" in {
    val path       = tempFile()
    val lines      = Seq("line 1 - !", "line 2 - ?", "line 3 - #")
    val lineWriter = Io.toLineWriter(path)

    lines.foreach(lineWriter.writeLine)
    lineWriter.flush()
    lineWriter.close()

    Io.readLines(path).toList should contain theSameElementsInOrderAs lines
  }
}
