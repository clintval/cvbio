package com.cvbio.commons

import com.fulcrumgenomics.commons.{CommonsDef => FgBioCommonsDef}
import htsjdk.samtools.BAMIndex.{BAMIndexSuffix => baiFileExtension}
import htsjdk.samtools.BamFileIoUtils.{BAM_FILE_EXTENSION => bamFileExtension}
import htsjdk.samtools.fastq.FastqConstants.FastqExtensions.{FASTQ => fastq, FQ => fq}

object CommonsDef extends FgBioCommonsDef {

  /** The extension of BAM index files. */
  val BaiExtension: FilenameSuffix = baiFileExtension

  /** The extension of BAM files. */
  val BamExtension: FilenameSuffix = bamFileExtension

  /** The long version of the FASTQ file extension. */
  val FastqExtension: FilenameSuffix = fastq.getExtension

  /** The short version of the FASTQ file extension. */
  val FqExtension: FilenameSuffix = fq.getExtension

  /** Represents a path to a GTF file. */
  type PathToGtf = java.nio.file.Path

  /** A String that represents a filename. */
  type Filename = String
}
