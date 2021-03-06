package io.cvbio.testing

import java.nio.file.Path

import io.cvbio.commons.effectful.Io
import com.fulcrumgenomics.commons.util.CaptureSystemStreams
import org.scalatest.{FlatSpec, Matchers, OptionValues, TryValues}

/** Base class for unit testing. */
trait UnitSpec extends FlatSpec with Matchers with OptionValues with TryValues with CaptureSystemStreams {

  /** Make an arbitrary temporary file with the following permissions. */
  def tempFile(readable: Boolean = true, writable: Boolean = true, executable: Boolean = true): Path = {
    val path = Io.makeTempFile(prefix = this.getClass.getSimpleName, suffix = ".txt")
    permissions(path, readable, writable, executable)
    path
  }

  /** Set permissions on the file underlying a file path. */
  private def permissions(path: Path, readable: Boolean = true, writable: Boolean = true, executable: Boolean = true):Path = {
    val file = path.toFile
    file.setReadable(readable)
    file.setWritable(writable)
    file.setExecutable(executable)
    path
  }
}
