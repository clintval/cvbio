package com.cvbio.commons.collection

import java.io.{BufferedWriter, OutputStreamWriter}

import com.cvbio.commons.io.Io
import com.cvbio.testing.UnitSpec

class BufferedLineWriterTest extends UnitSpec {

  "BufferedLineWriter" should "mixin with a BufferedWriter and write lines" in {
    val path       = tempFile()
    val lines      = Seq("line 1 - !", "line 2 - ?", "line 3 - #")
    val lineWriter = new BufferedWriter(new OutputStreamWriter(Io.toOutputStream(path))) with BufferedLineWriter

    lines.foreach(lineWriter.writeLine)
    lineWriter.flush()
    lineWriter.close()

    Io.readLines(path).toList should contain theSameElementsInOrderAs lines
  }
}
