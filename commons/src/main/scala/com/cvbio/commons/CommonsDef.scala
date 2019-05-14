package com.cvbio.commons

import com.fulcrumgenomics.commons.{CommonsDef => FgBioCommonsDef}

object CommonsDef extends FgBioCommonsDef {

  /** The extension of BAM index files. */
  val BaiExtension: FilenameSuffix = ".bai"

  /** The extension of BAM files. */
  val BamExtension: FilenameSuffix = ".bam"

  /** Represents a path to a GTF file. */
  type PathToGtf = java.nio.file.Path

  /** A String that represents a filename. */
  type Filename = String
}
