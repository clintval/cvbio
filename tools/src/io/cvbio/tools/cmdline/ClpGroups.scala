package io.cvbio.tools.cmdline

import com.fulcrumgenomics.sopt.cmdline.ClpGroup

/** The command line groups. */
object ClpGroups {

  class _Deprecated extends ClpGroup {
    override val name: String = "Deprecated"
    override val description: String = "These are provided for compatibility between major version upgrades."
  }

  class _SamOrBam extends ClpGroup {
    override val name: String = "SAM/BAM"
    override val description: String = "Tools for manipulating SAM, BAM, and related data."
  }

  class _Util extends ClpGroup {
    override val name: String = "Utilities"
    override val description: String = "Utility programs."
  }

  final val Deprecated = classOf[_Deprecated]
  final val SamOrBam   = classOf[_SamOrBam]
  final val Util       = classOf[_Util]
}
