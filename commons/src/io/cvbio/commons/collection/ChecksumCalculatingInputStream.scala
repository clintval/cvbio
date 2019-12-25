package io.cvbio.commons.collection

import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest

import io.cvbio.commons.collection.ChecksumCalculatingInputStream.{SentinelEmptyValue, Zero}

/** Calculate a checksum as a side-effect of consuming an input stream.
  *
  * Every implementation of the Java platform is required to support the following [[MessageDigest]] algorithms:
  *
  *   - `"MD5"`
  *   - `"SHA-1"`
  *   - `"SHA256"`
  *
  * @param inputStream the underlying input stream
  * @param algorithm the checksum algorithm
  * @param length the expected length of the checksum string, for md5 the length is 32
  */
abstract class ChecksumCalculatingInputStream(
  private val inputStream: InputStream,
  private val algorithm: String,
  private val length: Int
) extends InputStream {

  /** If the underlying stream is finished. */
  private var endOfStream: Boolean = false

  /** The message digest. */
  private val messageDigest: MessageDigest = MessageDigest.getInstance(algorithm)

  /** Reads the next byte of data from the input stream. The result is returned as an [[Int]] in the range 0 to 255. */
  override def read: Int = {
    val result = inputStream.read.ensuring(_.isValidByte)
    if (result == SentinelEmptyValue) endOfStream = true else messageDigest.update(result.toByte)
    result
  }

  /** Reads some number of bytes from the input stream and stores them into the buffer array <b>. */
  override def read(b: Array[Byte]): Int = {
    val result = inputStream.read(b).ensuring(_.isValidByte)
    if (result == SentinelEmptyValue) endOfStream = true else messageDigest.update(b, 0, result)
    result
  }

  /** Reads up to <len> bytes of data from the input stream into an array of bytes. */
  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    val result = inputStream.read(b, off, len).ensuring(_.isValidByte)
    if (result == SentinelEmptyValue) endOfStream = true else messageDigest.update(b, off, result)
    result
  }

  /** The message hash. Will exist after the input stream is exhausted. */
  def hash: Option[String] = if (!endOfStream) None else {
    val result = new BigInteger(1, messageDigest.digest).toString(16)
    Some(result.reverse.padTo(length, Zero).reverse)
  }

  /** Close the stream. */
  override def close(): Unit = {
    this.endOfStream = true
    inputStream.close()
  }

  /** This input stream does not support marking. */
  override def markSupported: Boolean = false

  /** This input stream does not support marking. */
  override def mark(readlimit: Int): Unit = {
    throw new UnsupportedOperationException(s"mark() is not supported by ${getClass.getSimpleName}.")
  }

  /** This input stream does not support resetting. */
  override def reset(): Unit = {
    throw new UnsupportedOperationException(s"reset() is not supported by ${getClass.getSimpleName}.")
  }

  /** This input stream does not support skipping. */
  def skip(readlimit: Int): Unit = {
    throw new UnsupportedOperationException(s"skip() is not supported by ${getClass.getSimpleName}.")
  }
}

/** Companion object to [[ChecksumCalculatingInputStream]]. */
object ChecksumCalculatingInputStream {

  /** The zero character. */
  private[collection] val Zero: Char = '0'

  /** The sentinel value that the Java platform will use to signal the end of a stream. */
  val SentinelEmptyValue: Int = -1

  /** Implicits for wrapping input streams for on-the-fly checksum calculation. */
  implicit class WithInputStreamUtil(private val inputStream: InputStream) {

    /** Implicitly wrap the input stream in a MD5 calculating input stream. */
    def md5Calculating: Md5CalculatingInputStream = new Md5CalculatingInputStream(inputStream)

    /** Implicitly wrap the input stream in a SHA-1 calculating input stream. */
    def sha1Calculating: Sha1CalculatingInputStream = new Sha1CalculatingInputStream(inputStream)

    /** Implicitly wrap the input stream in a SHA-256 calculating input stream. */
    def sha256Calculating: Sha256CalculatingInputStream = new Sha256CalculatingInputStream(inputStream)
  }
}

/** An MD5 checksum calculating input stream */
class Md5CalculatingInputStream(private val inputStream: InputStream)
  extends ChecksumCalculatingInputStream(inputStream, algorithm = "MD5", length = 32)

/** A SHA-1 checksum calculating input stream */
class Sha1CalculatingInputStream(private val inputStream: InputStream)
  extends ChecksumCalculatingInputStream(inputStream, algorithm = "SHA-1", length = 40)

/** A SHA-256 checksum calculating input stream */
class Sha256CalculatingInputStream(private val inputStream: InputStream)
  extends ChecksumCalculatingInputStream(inputStream, algorithm = "SHA-256", length = 64)
