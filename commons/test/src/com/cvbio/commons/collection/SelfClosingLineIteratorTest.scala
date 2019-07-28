package com.cvbio.commons.collection

import com.cvbio.commons.io.Io
import com.cvbio.testing.UnitSpec

import java.io.IOException

class SelfClosingLineIteratorTest extends UnitSpec {

  "BufferedLineWriter" should "mixin with a BufferedWriter and write lines" in {
    val path       = tempFile()
    val lines      = Seq("line 1 - !", "line 2 - ?", "line 3 - #")
    val lineWriter = Io.toLineWriter(path)

    lines.foreach(lineWriter.writeLine)
    lineWriter.flush()
    lineWriter.close()

    val reader   = Io.toReader(path)
    val iterator = new SelfClosingLineIterator(reader)

    iterator.toList should contain theSameElementsInOrderAs lines
    an[IOException] shouldBe thrownBy(reader.ready()) // Underlying Reader is now closed!
  }
}
