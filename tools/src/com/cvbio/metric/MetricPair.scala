package com.cvbio.metric

import com.cvbio.bam.Bams.ReadOrdinal.{Read1, Read2}
import com.cvbio.bam.Bams.TemplateUtil
import com.cvbio.commons.CommonsDef.SamTag
import com.cvbio.commons.MathUtil.WithMaxMinOption
import com.fulcrumgenomics.bam.Template

/** A pair of metrics, both optional, that can be collected from a fragment, single-end read, or paired-end read. */
class MetricPair[T: Ordering](read1: Option[T], read2: Option[T])
  extends Iterable[T]
  with Ordered[MetricPair[T]] {

  /** How to compare metric pairs. */
  override def compare(that: MetricPair[T]): Int = {
    Ordering.Tuple2(Ordering.Option, Ordering.Option).compare(
      (this.maxOption, this.minOption),
      (that.maxOption, that.minOption)
    )
  }

  /** Test if this [[MetricPair]] could be compared to another. */
  override def canEqual(that: Any): Boolean = that.isInstanceOf[MetricPair[T]]

  /** Test if this [[MetricPair]] is equal to another. */
  override def equals(that: Any): Boolean = that match {
    case _that: MetricPair[T] => this.compare(_that) == 0
    case _                    => false
  }

  /** An iterator over the value of the metric pair, if they are defined. */
  override def iterator: Iterator[T] = (read1 ++ read2).toIterator
}

/** Companion object for [[MetricPair]]. */
object MetricPair {

  /** Build a [[MetricPair]] from a [[Template]]. A function is required to reduce the tag values to one canonical value. */
  def apply[T](template: Template, tag: SamTag)(fn: (T, T) => T)(implicit cmp: Ordering[T]): MetricPair[T] = {
    new MetricPair(
      read1 = template.tagValues[T](Read1, tag).flatten.reduceOption(fn(_, _)),
      read2 = template.tagValues[T](Read2, tag).flatten.reduceOption(fn(_, _))
    )
  }

  /** Build an empty [[MetricPair]]. */
  def empty[T](implicit cmp: Ordering[T]): MetricPair[T] = new MetricPair[T](read1 = None, read2 = None)
}
