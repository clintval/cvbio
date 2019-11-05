package com.cvbio.tools.igv

/** Preference keys for IGV. */
object IgvPreferences {

  /** Preferences related to SAM files. */
  object Sam {

    // Track Display Options ///////////////////////////////////////////////////////////////////////////////////////////

    val ShowAlignmentTrack: String = "SAM.SHOW_ALIGNMENT_TRACK"

    val ShowCoverageTrack: String = "SAM.SHOW_COV_TRACK"

    val ShowJunctionTrack: String = "SAM.SHOW_JUNCTION_TRACK"

    // Downsampling ////////////////////////////////////////////////////////////////////////////////////////////////////

    /** The preference for downsampling reads. */
    val DownsampleReads: String = "SAM.DOWNSAMPLE_READS"

    /** The preference for a downsampling window. */
    val DownsampleWindow: String = "SAM.SAMPLING_WINDOW"

    /** The preference for a the number of reads in a downsampling window. */
    val NumberOfReadsPerWindow: String = "SAM.MAX_LEVELS"

    // Alignment Track Options /////////////////////////////////////////////////////////////////////////////////////////

    val MaximumBaseQualityToShade: String = "SAM.BASE_QUALITY_MAX"

    val MinimumBaseQualityToShade: String = "SAM.BASE_QUALITY_MIN"
  }
}
