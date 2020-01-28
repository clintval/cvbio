package io.cvbio.tools.cmdline

import com.fulcrumgenomics.sopt.cmdline.ClpGroup

/** The command line groups. */
object ClpGroups {

  class _Deprecated extends ClpGroup {
    override val name: String = "Deprecated"
    override val description: String = "These tools are provided for compatibility between major version upgrades."
  }

  class _SamOrBam extends ClpGroup {
    override val name: String = "SAM/BAM"
    override val description: String = "Tools for manipulating SAM, BAM, and related data."
  }

  class _Util extends ClpGroup {
    override val name: String = "Utilities"
    override val description: String = "Utility programs."
  }

  class _VcfOrBcf extends ClpGroup {
    override val name: String = "VCF/BCF"
    override val description: String = "Tools for manipulating VCF, BCF, and related data."
  }

  final val Deprecated = classOf[_Deprecated]
  final val SamOrBam   = classOf[_SamOrBam]
  final val Util       = classOf[_Util]
  final val VcfOrBcf   = classOf[_VcfOrBcf]
}
