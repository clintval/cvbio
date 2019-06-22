package com.cvbio.pipeline.tasks.star

import java.nio.file.Path

import dagr.core.config.Configuration

/** Constants and defaults that are used across all invocations of the `STAR` aligner */
object Star extends Configuration {

  /** The configuration key for accessing the `STAR` executable. */
  val StarExecutableConfigKey: String = "star.executable"

  /** Find the path to the `STAR` executable. */
  def findStar: Path = configureExecutable(StarExecutableConfigKey, "STAR")
}
