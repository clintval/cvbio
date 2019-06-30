package com.cvbio.commons

import com.fulcrumgenomics.commons.io.PathUtil
import com.fulcrumgenomics.commons.{CommonsDef => FgBioCommonsDef}
import htsjdk.samtools.BAMIndex.{BAMIndexSuffix => baiFileExtension}
import htsjdk.samtools.BamFileIoUtils.{BAM_FILE_EXTENSION => bamFileExtension}
import htsjdk.samtools.cram.CRAIIndex.{CRAI_INDEX_SUFFIX => craiFileExtension}
import htsjdk.samtools.cram.build.CramIO.{CRAM_FILE_EXTENSION => cramFileExtension}
import htsjdk.samtools.fastq.FastqConstants.FastqExtensions.{FASTQ => fastq, FQ => fq}

object CommonsDef extends FgBioCommonsDef {

  /** The extension of BAM index files. */
  val BaiExtension: FilenameSuffix = baiFileExtension

  /** The extension of BAM files. */
  val BamExtension: FilenameSuffix = bamFileExtension

  /** The extension of CRAM index files. */
  val CraiExtension: FilenameSuffix = craiFileExtension

  /** The extension of CRAM files. */
  val CramExtension: FilenameSuffix = cramFileExtension

  /** The long version of the FASTQ file extension. */
  val FastqExtension: FilenameSuffix = fastq.getExtension

  /** The short version of the FASTQ file extension. */
  val FqExtension: FilenameSuffix = fq.getExtension

  /** Return the path to a BAM index file of the form `<filename>.bai` */
  def bai(path: PathToBam): PathToBai = PathUtil.replaceExtension(path, BaiExtension)

  /** Return the path to a BAM index file of the form `<filename>.bam.bai` */
  def bamBai(path: PathToBam): PathToBai = PathUtil.replaceExtension(path, BamExtension + BaiExtension)

  /** Return the path to a BAM index file of the form `<filename>.crai` */
  def crai(path: PathToBam): PathToBai = PathUtil.replaceExtension(path, craiFileExtension)

  /** Return the path to a BAM index file of the form `<filename>.cram.crai` */
  def cramCrai(path: PathToBam): PathToBai = PathUtil.replaceExtension(path, cramFileExtension + craiFileExtension)

  /** Represents a path to a BAM/CRAM index file. */
  type PathToBai = java.nio.file.Path

  /** Represents a path to a GTF file. */
  type PathToGtf = java.nio.file.Path

  /** A String that represents a filename. */
  type Filename = String
}
