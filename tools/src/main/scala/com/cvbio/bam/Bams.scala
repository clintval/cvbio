package com.cvbio.bam

import com.cvbio.commons.CommonsDef._
import com.fulcrumgenomics.bam.Bams.templateIterator
import com.fulcrumgenomics.bam.Template
import com.fulcrumgenomics.bam.api.SamSource
import com.fulcrumgenomics.commons.collection.SelfClosingIterator
import com.fulcrumgenomics.commons.util.LazyLogging
import htsjdk.samtools.SAMFileHeader.SortOrder

/** Common methods for working with SAM/BAM files. */
object Bams extends LazyLogging {

  /** Zip a collection of template iterators containing the same template, albeit with potentially different alignments.
    *
    * All [[SamSource]]'s must be query name sorted and all templates must be synchronized by read name.
    * */
  def templatesIterator(sources: SamSource*): SelfClosingIterator[Seq[Template]] = {
    val templateIterators = sources
      .map { samSource: SamSource =>
        require(queryNameSorted(samSource), s"SAM source is not queryname sorted: $samSource")
        templateIterator(in = samSource)
      }

    val iterator = new Iterator[Seq[Template]] {
      override def hasNext: Boolean = templateIterators.map(_.hasNext).forall(_ == true)
      override def next(): Seq[Template] = {
        require(hasNext, "next() called on empty iterator")
        val templates = templateIterators.map(_.next)
        require(templates.map(_.name).distinct.length == 1, s"Templates do not have the same name: " + templates.mkString(", "))
        templates
      }
    }

    new SelfClosingIterator(iterator, () => sources.foreach(_.close()))
  }

  /** Test if a [[SamSource]] is queryname sorted.
    *
    * @throws IllegalStateException when no sort order is defined.
    * */
  def queryNameSorted(source: SamSource): Boolean = {
    Option(source.header.getSortOrder).getOrElse {
      source.safelyClose()
      throw new IllegalStateException(s"No sort order is defined for SAM source: $source")
    } == SortOrder.queryname
  }
}
