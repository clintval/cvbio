package io.cvbio.commons.collection

import java.io.ByteArrayInputStream

import com.fulcrumgenomics.commons.io.Io
import htsjdk.samtools.util.{Md5CalculatingInputStream => HtsJdkMd5CalculatingInputStream}
import io.cvbio.testing.UnitSpec
import org.apache.commons.io.Charsets

import scala.io.Source

class ChecksumCalculatingInputStreamTest extends UnitSpec {

  "Md5CalculatingInputStream" should "behave the same as htsjdk.samtools.util.Md5CalculatingInputStream" in {

    Seq(
      ("Vf3ppC4Iu74AAAAAaHR0cDovL2hhc2hjYXQubmV0LwA", "cdfed259056f77f3ffed384466a8bbdf"),
      ("",                                            "d41d8cd98f00b204e9800998ecf8427e"),
      ("a",                                           "0cc175b9c0f1b6a831c399e269772661"),
      ("jk8ssl",                                      "0000000018e6137ac2caab16074784a6")
    ).foreach { case (string: String, expected: String) =>
      val inputBytes  = string.getBytes(Charsets.US_ASCII)
      val cvbioStream = new Md5CalculatingInputStream(new ByteArrayInputStream(inputBytes))
      val otherStream = new HtsJdkMd5CalculatingInputStream(new ByteArrayInputStream(inputBytes), Io.DevNull.toFile)

      cvbioStream.hash shouldBe None

      Source.fromInputStream(cvbioStream).toList.mkString("") shouldBe string
      Source.fromInputStream(otherStream).toList.mkString("") shouldBe string

      cvbioStream.read shouldBe -1
      otherStream.read shouldBe -1

      otherStream.close() // Isn't our API just a little nicer?

      otherStream.md5        shouldBe expected
      cvbioStream.hash.value shouldBe expected
      cvbioStream.hash.value.length shouldBe 32
    }
  }

  it should "compute a checksum when the stream is closed" in {
    val string = "test"
    val stream = new Md5CalculatingInputStream(new ByteArrayInputStream(string.getBytes(Charsets.US_ASCII)))
    stream.hash shouldBe None
    string.map(_ => stream.read) // Read exactly the number of bytes in the stream and no more!
    stream.hash shouldBe None
    stream.close()               // Signal the stream is finished by closing it.
    stream.hash.value shouldBe "098f6bcd4621d373cade4e832627b4f6"
  }

  it should "successfully read a single byte" in {
    val string = "test"
    val stream = new Md5CalculatingInputStream(new ByteArrayInputStream(string.getBytes(Charsets.US_ASCII)))
    stream.hash shouldBe None
    string.map(_ => stream.read) shouldBe string.map(_.toByte.toInt)
    stream.close()
    stream.hash.value shouldBe "098f6bcd4621d373cade4e832627b4f6"
  }

  it should "successfully read and store an array of bytes" in {
    val string = "test"
    val stream = new Md5CalculatingInputStream(new ByteArrayInputStream(string.getBytes(Charsets.US_ASCII)))
    val array  = new Array[Byte](4)
    stream.read(array) shouldBe 4
    stream.hash shouldBe None
    stream.read(array) shouldBe -1
    array shouldBe string.map(_.toByte.toInt).toArray
    stream.hash.value shouldBe "098f6bcd4621d373cade4e832627b4f6"
  }

  it should "successfully read and store an array of bytes with a fixed offset" in {
    val string = "test"
    val stream = new Md5CalculatingInputStream(new ByteArrayInputStream(string.getBytes(Charsets.US_ASCII)))
    val array  = new Array[Byte](1)
    stream.read(array, 0, 1) shouldBe 1
    array shouldBe string.map(_.toByte.toInt).take(1)
    stream.hash shouldBe None
  }

  it should "raise exceptions when using unsupported features" in {
    val string = "test"
    val stream = new Md5CalculatingInputStream(new ByteArrayInputStream(string.getBytes(Charsets.US_ASCII)))
    stream.markSupported shouldBe false
    an[UnsupportedOperationException] shouldBe thrownBy { stream.mark(0) }
    an[UnsupportedOperationException] shouldBe thrownBy { stream.reset() }
    an[UnsupportedOperationException] shouldBe thrownBy { stream.skip(0) }
  }

  "Sha1CalculatingInputStream" should "calculate the correct hashes" in {

    Seq(
      ("Vf3ppC4Iu74AAAAAaHR0cDovL2hhc2hjYXQubmV0LwA", "3af2f9159cb82aa55fa4bb8488cb0767a0630bbb"),
      ("",                                            "da39a3ee5e6b4b0d3255bfef95601890afd80709"),
      ("a",                                           "86f7e437faa5a7fce15d1ddcb9eaeaea377667b8"),
      ("jk8ssl",                                      "8b5487a85ceb69902773d30c843d5015f2a4e3b6")
    ).foreach { case (string: String, expected: String) =>
      val inputBytes = string.getBytes(Charsets.US_ASCII)
      val stream     = new Sha1CalculatingInputStream(new ByteArrayInputStream(inputBytes))

      Source.fromInputStream(stream).toList.mkString("") shouldBe string
      stream.read shouldBe -1
      stream.hash.value shouldBe expected
      stream.hash.value.length shouldBe 40
    }
  }

  "Sha256CalculatingInputStream" should "calculate the correct hashes" in {

    Seq(
      ("Vf3ppC4Iu74AAAAAaHR0cDovL2hhc2hjYXQubmV0LwA", "50e07769c8763d8939a8dc55bc59799bb46c314193b4eee4ee0776d7cf5f08e4"),
      ("",                                            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
      ("a",                                           "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb"),
      ("jk8ssl",                                      "f07f9e2b2bb62bdbbda731319f19cec91f369ce1f6cb1d6ba8e8b962d1c73d3a")
    ).foreach { case (string: String, expected: String) =>
      val inputBytes = string.getBytes(Charsets.US_ASCII)
      val stream     = new Sha256CalculatingInputStream(new ByteArrayInputStream(inputBytes))

      Source.fromInputStream(stream).toList.mkString("") shouldBe string
      stream.read shouldBe -1
      stream.hash.value shouldBe expected
      stream.hash.value.length shouldBe 64
    }
  }
}
