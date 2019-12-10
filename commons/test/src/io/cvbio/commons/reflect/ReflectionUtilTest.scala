package io.cvbio.commons.reflect

import com.fulcrumgenomics.commons.io.PathUtil
import io.cvbio.testing.UnitSpec

class ReflectionUtilTest extends UnitSpec {

  "ReflectionUtil.resourceListing" should "find this very file in the listing" in {
    val paths = ReflectionUtil.resourceListing(getClass).map(PathUtil.pathTo(_)).toList
    paths.map(path => PathUtil.basename(path, trimExt = true)) should contain (getClass.getSimpleName)
  }
}
