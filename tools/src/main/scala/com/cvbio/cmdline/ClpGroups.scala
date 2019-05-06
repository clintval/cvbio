package com.cvbio.cmdline

import com.fulcrumgenomics.sopt.cmdline.ClpGroup

/** The command line groups. */
object ClpGroups {

  class _Ensembl extends ClpGroup {
    override val name: String = "Ensembl"
    override val description: String = "Routines for downloading and formatting Ensembl data."
  }

  final val Ensembl = classOf[_Ensembl]
}
