package io.cvbio.pipelines.tasks.misc

import java.nio.file.Files
import java.nio.file.StandardCopyOption.{REPLACE_EXISTING => replaceExisting}

import io.cvbio.commons.CommonsDef._
import com.fulcrumgenomics.commons.io.PathUtil
import dagr.core.tasksystem.{FixedResources, SimpleInJvmTask}

/** Move a BAM/CRAM file and its associated indexes if they exist. */
class MoveBam(source: PathToBam, target: PathToBam) extends SimpleInJvmTask with FixedResources {

  /** The commands to run. */
  override def run: Unit = {
    Files.move(source, target, replaceExisting)

    PathUtil.extensionOf(source) match {
      case Some(BamExtension) =>
        if (bai(source).toFile.exists) Files.move(bai(source), bai(target), replaceExisting)
        if (bamBai(source).toFile.exists) Files.move(bamBai(source), bamBai(target), replaceExisting)
      case Some(CramExtension) =>
        if (crai(source).toFile.exists) Files.move(crai(source), crai(target), replaceExisting)
        if (cramCrai(source).toFile.exists) Files.move(cramCrai(source), cramCrai(target), replaceExisting)
      case _ =>
    }
  }
}
