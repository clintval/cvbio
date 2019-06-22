package com.cvbio.bam

import com.fulcrumgenomics.bam.Bams.templateIterator
import com.fulcrumgenomics.bam.Template
import com.fulcrumgenomics.bam.api.SamOrder.Queryname
import com.fulcrumgenomics.bam.api.SamSource
import com.fulcrumgenomics.commons.CommonsDef._
import com.fulcrumgenomics.commons.collection.SelfClosingIterator
import com.fulcrumgenomics.commons.util.LazyLogging

/** Common methods for working with SAM/BAM files. */
object Bams extends LazyLogging {

  /** Collectively iterate through [[SamSource]] iterators and emit templates of the same name, albeit with potentially
    * different alignments.
    *
    * All [[SamSource]]'s must be query name sorted and all templates must be synchronized by read name.
    * */
  def templatesIterator(sources: SamSource*): SelfClosingIterator[Seq[Template]] = {
    val templateIterators = sources
      .map { source: SamSource =>
        require(querynameSorted(source), s"SAM source is not queryname sorted: $source")
        templateIterator(in = source)
      }

    val iterator = new Iterator[Seq[Template]] {

      /** Check to see if all underlying iterators have another [[com.fulcrumgenomics.bam.Template]] or not. */
      override def hasNext: Boolean = {
        val allHasNext = templateIterators.map(_.hasNext)
        require(allHasNext.distinct.length <= 1, "SAM sources do not have the same number of templates")
        if (allHasNext.isEmpty) false else allHasNext.forall(_ == true)
      }

      /** Grab the next sequence of [[com.fulcrumgenomics.bam.Template]]. */
      override def next(): Seq[Template] = {
        require(hasNext, "next() called on empty iterator")
        val templates = templateIterators.map(_.next)
        require(templates.map(_.name).distinct.length <= 1, s"Templates do not have the same name: " + templates.mkString(", "))
        templates
      }
    }

    new SelfClosingIterator(iterator, () => sources.foreach(_.safelyClose()))
  }

  /** Test if a [[SamSource]] is queryname sorted. */
  private[cvbio] def querynameSorted(source: SamSource): Boolean = Option(source.header.getSortOrder).contains(Queryname.sortOrder)
}
