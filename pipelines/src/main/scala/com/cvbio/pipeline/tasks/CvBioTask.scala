package com.cvbio.pipeline.tasks

import dagr.core.config.Configuration
import dagr.core.execsystem.{Cores, Memory}
import dagr.core.tasksystem.{FixedResources, ProcessTask}
import dagr.tasks.JarTask

/** All `cvbio` tools should extend from this task. */
abstract class CvBioTask extends ProcessTask with JarTask with FixedResources with Configuration {
  requires(Cores(1), Memory("2G"))
}
