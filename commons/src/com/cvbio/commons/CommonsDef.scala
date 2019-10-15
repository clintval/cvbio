package com.cvbio.commons

import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.commons.{CommonsDef => FgBioCommonsDef}
import htsjdk.samtools.fastq.FastqConstants.FastqExtensions.{FASTQ => fastq, FQ => fq}
import htsjdk.samtools.util.FileExtensions

object CommonsDef extends FgBioCommonsDef {

  /** The extension of BAM index files. */
  val BaiExtension: FilenameSuffix = FileExtensions.BAI_INDEX

  /** The extension of BAM files. */
  val BamExtension: FilenameSuffix = FileExtensions.BAM

  /** The extension of CRAM index files. */
  val CraiExtension: FilenameSuffix = FileExtensions.CRAM_INDEX

  /** The extension of CRAM files. */
  val CramExtension: FilenameSuffix = FileExtensions.CRAM

  /** The extension of SAM files. */
  val SamExtension: FilenameSuffix = FileExtensions.SAM

  /** The long version of the FASTQ file extension. */
  val FastqExtension: FilenameSuffix = fastq.getExtension

  /** The short version of the FASTQ file extension. */
  val FqExtension: FilenameSuffix = fq.getExtension

  /** Implicits for self returning side-effecting code.
    *
    * @param self The collection to tap
    * @tparam T The type within the collection
    * */
  implicit class Tap[T](self: TraversableOnce[T]) {

    /** Apply a side-effecting function to a collection and then allow further method chaining. */
    def tapEach[U](fn: T => U): TraversableOnce[T] = self.map { item: T => fn(item); item }
  }

  /** Cast a tuple of [[Option]]s in which only one is set, into an [[Either]].
    *
    * @throws IllegalStateException when both Options are set or neither are set.
    */
  def optionToEither[A, B](tuple: (Option[A], Option[B])): Either[A, B] = tuple match {
    case (Some(l), None) => println(l); Left(l)
    case (None, Some(r)) => println(r); Right(r)
    case (_, _)          => throw new IllegalStateException(s"Options are not mutually exclusive and one was not set: $tuple")
  }

  /** Insert a separating item after every item. */
  def interleave[T](sep: T): Seq[T] => Seq[T] = (seq: Seq[T]) => seq.flatMap(Seq(_, sep))

  /** Return the path to a BAM index file of the form `<filename>.bai` */
  def bai(path: PathToBam): PathToBai = PathUtil.replaceExtension(path, BaiExtension)

  /** Return the path to a BAM index file of the form `<filename>.bam.bai` */
  def bamBai(path: PathToBam): PathToBai = PathUtil.replaceExtension(path, BamExtension + BaiExtension)

  /** Return the path to a BAM index file of the form `<filename>.crai` */
  def crai(path: PathToBam): PathToBai = PathUtil.replaceExtension(path, CraiExtension)

  /** Return the path to a BAM index file of the form `<filename>.cram.crai` */
  def cramCrai(path: PathToBam): PathToBai = PathUtil.replaceExtension(path, CramExtension + CraiExtension)

  /** A String that represents a filename. */
  type Filename = String

  /** Represents a path to a BAM/CRAM index file. */
  type PathToBai = java.nio.file.Path

  /** Represents a path to a BigWig file. */
  type PathToBigWig = java.nio.file.Path

  /** Represents a path to a GTF file. */
  type PathToGtf = java.nio.file.Path

  /** Represents a path to an Illumina Sample Sheet. */
  type PathToSampleSheet = java.nio.file.Path

  /** Represents a SAM tag. */
  type SamTag = String
}
