package io.cvbio.pipelines.cmdline

import com.fulcrumgenomics.sopt.cmdline.ClpGroup

/** The command line groups. */
object ClpGroups {

  class _AdHoc extends ClpGroup {
    override val name: String = "AdHoc Pipelines"
    override val description: String = "AdHoc pipelines without a home yet."
  }

  class _SamOrBam extends ClpGroup {
    override val name: String = "SAM/BAM"
    override val description: String = "Tools for manipulating SAM, BAM, or related data."
  }

  class _Utilities extends ClpGroup {
    override val name: String = "Utilities"
    override val description: String = "Utility and reference data management tools"
  }

  final val AdHoc    = classOf[_AdHoc]
  final val SamOrBam = classOf[_SamOrBam]
  final val Utility  = classOf[_Utilities]
}
