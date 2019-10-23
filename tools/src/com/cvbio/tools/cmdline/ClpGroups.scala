package com.cvbio.tools.cmdline

import com.fulcrumgenomics.sopt.cmdline.ClpGroup

/** The command line groups. */
object ClpGroups {

  class _Ensembl extends ClpGroup {
    override val name: String = "Ensembl"
    override val description: String = "Tools for downloading and formatting Ensembl data."
  }

  class _Igv extends ClpGroup {
    override val name: String = "IGV"
    override val description: String = "Tools for working with the Integrated Genomics Viewer."
  }

  class _SamOrBam extends ClpGroup {
    override val name: String = "SAM/BAM"
    override val description: String = "Tools for manipulating SAM, BAM, and related data."
  }

  final val Ensembl  = classOf[_Ensembl]
  final val Igv      = classOf[_Igv]
  final val SamOrBam = classOf[_SamOrBam]
}
