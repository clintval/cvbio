package com.cvbio.pipeline.tasks

import java.nio.file.Path

import dagr.core.config.Configuration
import dagr.core.execsystem.{Cores, Memory}
import dagr.core.tasksystem.{FixedResources, ProcessTask}
import dagr.tasks.JarTask

import scala.collection.mutable.ListBuffer

/** Companion object to [[CvBioTask]]. */
object CvBioTask {

  /** The configuration path for the `cvbio` JAR. */
  val CvBioJarConfigPath = "cvbio.jar"
}

/** All `cvbio` tools should extend from this task. */
abstract class CvBioTask extends ProcessTask with JarTask with FixedResources with Configuration {
  requires(Cores(1), Memory("2G"))

  /** Look up the command name. */
  val commandName: String = JarTask.findCommandName(getClass)

  /** The name of the task. */
  name = commandName

  /** The file path to the `cvbio` JAR. */
  protected def cvBioJar: Path = configure[Path](CvBioTask.CvBioJarConfigPath)

  /** The command line arguments. */
  override final def args: Seq[Any] = {
    val buffer = ListBuffer[Any]()
    buffer.appendAll(jarArgs(cvBioJar, jvmMemory = resources.memory))
    buffer.append(commandName)
    addCvBioArgs(buffer)
    buffer
  }

  /** The method to implement for adding command line arguments to a `cvbio` JAR. */
  protected def addCvBioArgs(buffer: ListBuffer[Any]): Unit
}
