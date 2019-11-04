package com.cvbio.tools.cmdline

import com.fulcrumgenomics.sopt.cmdline.ClpGroup

/** The command line groups. */
object ClpGroups {

  class _Util extends ClpGroup {
    override val name: String = "Utilities"
    override val description: String = "Utility programs."
  }

  class _SamOrBam extends ClpGroup {
    override val name: String = "SAM/BAM"
    override val description: String = "Tools for manipulating SAM, BAM, and related data."
  }

  final val SamOrBam = classOf[_SamOrBam]
  final val Util     = classOf[_Util]
}
