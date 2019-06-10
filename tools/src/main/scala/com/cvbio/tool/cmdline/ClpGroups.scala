package com.cvbio.tool.cmdline

import com.fulcrumgenomics.sopt.cmdline.ClpGroup

/** The command line groups. */
object ClpGroups {

  class _AdHoc extends ClpGroup {
    override val name: String = "AdHoc"
    override val description: String = "AdHoc tooling without a home yet."
  }

  class _Ensembl extends ClpGroup {
    override val name: String = "Ensembl"
    override val description: String = "Tools for downloading and formatting Ensembl data."
  }

  class _SamOrBam extends ClpGroup {
    override val name: String = "SAM/BAM"
    override val description: String = "Tools for manipulating SAM, BAM, or related data."
  }

  final val AdHoc    = classOf[_AdHoc]
  final val Ensembl  = classOf[_Ensembl]
  final val SamOrBam = classOf[_SamOrBam]
}
