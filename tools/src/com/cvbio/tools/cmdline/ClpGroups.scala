package com.cvbio.tools.cmdline

import com.fulcrumgenomics.sopt.cmdline.ClpGroup

/** The command line groups. */
object ClpGroups {

  class _SamBam extends ClpGroup {
    override val name: String = "SAM/BAM"
    override val description: String = "Tools for manipulating SAM, BAM, and related data."
  }

  class _Ensembl extends ClpGroup {
    override val name: String = "Ensembl"
    override val description: String = "Tools for downloading and formatting Ensembl data."
  }

  final val AdHoc   = classOf[_SamBam]
  final val Ensembl = classOf[_Ensembl]
}
