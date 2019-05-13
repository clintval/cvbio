package com.cvbio.pipeline.tasks.star

import com.cvbio.commons.CommonsDef._
import dagr.core.execsystem.{Cores, Memory}
import dagr.core.tasksystem.{FixedResources, ProcessTask}

import scala.collection.mutable.ListBuffer

/** Defaults that are used when aligning with `STAR` */
object StarAlign {

  /** The filename suffix `STAR` uses for the coordinate sorted output BAM. */
  val AlignedCoordinateSortedSuffix: FilenameSuffix = "Aligned.sortedByCoord.out.bam"

  /** The default cores to use. */
  val DefaultCores = Cores(6)

  /** The default memory to use. */
  val DefaultMemory = Memory("24G")
}

/** Align paired end reads from FASTQ files with the `STAR` aligner. */
class StarAlign(
  read1: PathToFastq,
  read2: Option[PathToFastq],
  genomeDir: DirPath,
  prefix: Option[PathPrefix] = None,
  cores: Cores = StarAlign.DefaultCores
) extends ProcessTask
  with FixedResources {

  requires(cores, StarAlign.DefaultMemory)

  /** The command line arguments. */
  override def args: Seq[Any] = {
    val buffer = ListBuffer[Any]()
    buffer.append(Star.findStar)
    buffer.append("--runThreadN", resources.cores)
    buffer.append("--runMode", "alignReads")
    buffer.append(Seq("--readFilesIn") ++ readFiles: _*)
    buffer.append("--genomeDir", genomeDir)
    prefix.foreach(buffer.append("--outFileNamePrefix", _))
    buffer.append("--outSAMtype", "BAM", "SortedByCoordinate")
    buffer
  }

  /** Format the FASTQ file paths argument. */
  private def readFiles: Seq[String] = {
    val files = read2 match {
      case Some(_read2) => Seq(read1, _read2)
      case None         => Seq(read1)
    }
    files.map(_.toString)
  }
}
