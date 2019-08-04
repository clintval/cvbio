package com.cvbio.tools.disambiguate

import com.cvbio.metric.MetricPair
import com.fulcrumgenomics.FgBioDef.FgBioEnum
import com.fulcrumgenomics.bam.Template
import enumeratum.EnumEntry
import htsjdk.samtools.SAMTag.{AS, NM}

/** Trait that all enumeration values of type [[TemplateOrdering]] should extend. */
sealed trait TemplateOrdering extends EnumEntry with Ordering[Template]

/** Contains enumerations of template orderings. */
object TemplateOrdering extends FgBioEnum[TemplateOrdering] {

  /** Return all template orderings. */
  def values: scala.collection.immutable.IndexedSeq[TemplateOrdering] = findValues

  /** The value when [[TemplateOrdering]] is the original published ordering.
    *
    * A [[Template]] should be ordered before another [[Template]] if it has:
    *
    * 1. The highest single max alignment score across read one and read two, if defined.
    * 2. The highest single min alignment score across read one and read two, if defined.
    * 3. The lowest single min alignment edit distance across read one and read two, if defined.
    * 3. The lowest single max alignment edit distance across read one and read two, if defined.
    *
    * If neither template is clearly better, then the templates are equivalent.
    * */
  case object ClassicOrdering extends TemplateOrdering {

    /** Compare two templates using the original published algorithm. */
    override def compare(x: Template, y: Template): Int = {
      import com.cvbio.commons.CommonsDef.SamTagConversions._

      val alignmentScore = (template: Template) => MetricPair[Int](template, AS)(_ max _)
      val numMismatches  = (template: Template) => MetricPair[Int](template, NM)(_ min _)

      var compare = alignmentScore(x).compare(alignmentScore(y))
      if (compare == 0) compare = -numMismatches(x).compare(numMismatches(y)) // Negate because less is better.
      compare
    }
  }
}
