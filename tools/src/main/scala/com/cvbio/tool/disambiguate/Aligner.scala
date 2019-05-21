package com.cvbio.tool.disambiguate

import com.fulcrumgenomics.FgBioDef.FgBioEnum
import enumeratum.EnumEntry

/** Trait that all enumeration values of type [[Aligner]] should extend. */
sealed trait Aligner extends EnumEntry

/** Contains enumerations of various aligners. */
object Aligner extends FgBioEnum[Aligner] {

  def values: scala.collection.immutable.IndexedSeq[Aligner] = findValues

  /** The value when [[Aligner]] is the Burrows-Wheeler Aligner. */
  case object Bwa extends Aligner

  /** The value when [[Aligner]] is the `STAR` Aligner. */
  case object Star extends Aligner
}
