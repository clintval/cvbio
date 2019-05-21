package com.cvbio.tool.disambiguate

import com.cvbio.commons.CommonsDef._
import com.fulcrumgenomics.bam.Bams.templateIterator
import com.fulcrumgenomics.bam.Template
import com.fulcrumgenomics.bam.api.SamSource
import htsjdk.samtools.SAMFileHeader.SortOrder

/** A collection of two [[Template]] instances that were aligned to different genomes. */
case class AlignmentPair(t1: Template, t2: Template) {
  require(t1.name == t2.name, s"Both templates must have the same name: Template 1: $t1  Template 2: $t2")

  /** Test that both [[Template]] instances have read 1 defined. */
  def bothHaveR1: Boolean = t1.r1.isDefined && t2.r2.isDefined
}

/** Companion object for [[AlignmentPair]]. */
object AlignmentPair extends ((Template, Template) => AlignmentPair) {

  /** Build an iterator of [[AlignmentPair]] from two queryname-sorted [[SamSource]] instances.
    *
    * @throws IllegalStateException when either [[SamSource]] is not queryname-sorted.
    */
  def apply(source1: SamSource, source2: SamSource): Iterator[AlignmentPair] = {
    Seq(source1, source2).foreach(source => require(queryNameSorted(source), s"SAM source is not query name sorted: $source"))
    templateIterator(source1).zip(templateIterator(source2)).map(AlignmentPair.tupled)
  }

  /** Test if a [[SamSource]] is queryname sorted.
    *
    * @throws IllegalStateException when no sort order is defined.
    */
  private[disambiguate] def queryNameSorted(source: SamSource): Boolean = {
    Option(source.header.getSortOrder).getOrElse {
      source.safelyClose()
      throw new IllegalStateException(s"No sort order is defined for SAM source: $source")
    } == SortOrder.queryname
  }
}
