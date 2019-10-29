package io.cvbio.tools.cmdline

import com.fulcrumgenomics.cmdline.FgBioTool
import com.fulcrumgenomics.commons.util.LazyLogging

/** The trait that all `cvbio` tools should extend. */
trait CvBioTool extends FgBioTool with LazyLogging
