package com.cvbio.pipeline.cmdline

import com.fulcrumgenomics.sopt.cmdline.ClpGroup

/** The command line groups. */
object ClpGroups {

  class _AdHoc extends ClpGroup {
    override val name: String = "AdHoc Pipelines"
    override val description: String = "AdHoc pipelines without a home yet."
  }

  class _ReferenceData extends ClpGroup {
    override val name: String = "Reference Data"
    override val description: String = "Pipelines for downloading and preparing reference data."
  }

  class _Rna extends ClpGroup {
    override val name: String = "RNA"
    override val description: String = "Pipelines for RNA or cDNA processing"
  }

  final val AdHoc         = classOf[_AdHoc]
  final val ReferenceData = classOf[_ReferenceData]
  final val Rna           = classOf[_Rna]
}
