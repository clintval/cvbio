package com.cvbio.tools.disambiguate

import com.cvbio.commons.MathUtil
import com.cvbio.tools.disambiguate.TemplateOrdering.ClassicOrdering
import com.fulcrumgenomics.FgBioDef.FgBioEnum
import com.fulcrumgenomics.bam.Template
import enumeratum.EnumEntry

/** Trait that all enumeration values of type [[DisambiguationStrategy]] should extend. */
sealed trait DisambiguationStrategy extends EnumEntry with Product with Serializable {

  /** Take in a sequence of templates and return the the one with the most optimal alignment. */
  def choose(templates: Seq[Template]): Option[Template]
}

/** Contains enumerations of template disambiguation strategies. */
object DisambiguationStrategy extends FgBioEnum[DisambiguationStrategy] {

  /** Return all disambiguation strategies. */
  def values: scala.collection.immutable.IndexedSeq[DisambiguationStrategy] = findValues

  /** Test if all reads in a template are unmapped or not. */
  private def unmapped(template: Template): Boolean = template.allReads.forall(_.unmapped == true)

  /** The value when [[DisambiguationStrategy]] is the original published algorithm. */
  case object Classic extends DisambiguationStrategy {

    /** Choose the template which is solely the best after ordering by the original published algorithm. */
    def choose(templates: Seq[Template]): Option[Template] = {
      MathUtil.pickMax(templates.filterNot(unmapped))(ClassicOrdering)
    }
  }
}
