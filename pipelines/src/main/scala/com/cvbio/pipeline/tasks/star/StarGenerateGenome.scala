package com.cvbio.pipeline.tasks.star

import com.cvbio.commons.CommonsDef._
import dagr.core.execsystem.{Cores, Memory}
import dagr.core.tasksystem.{FixedResources, ProcessTask}

import scala.collection.mutable.ListBuffer

/** Prepare reference data for the `STAR` aligner. */
class StarGenerateGenome(
  fasta: Seq[PathToFasta],
  gtf: PathToGtf,
  outputDir: DirPath,
  overhang: Int = 100,
  prefix: Option[PathPrefix] = None,
  threads: Cores = StarGenerateGenome.DefaultCores
) extends ProcessTask
  with FixedResources {

  requires(threads, StarGenerateGenome.DefaultMemory)

  /** The command line arguments. */
  override def args: Seq[Any] = {
    val buffer = ListBuffer[Any]()
    buffer.append(Star.findStar)
    buffer.append("--runThreadN", resources.cores)
    buffer.append("--runMode", "genomeGenerate")
    buffer.append("--genomeFastaFiles", fasta.mkString(" "))
    buffer.append("--genomeDir", outputDir)
    buffer.append("--sjdbGTFfile", gtf)
    buffer.append("--sjdbOverhang", overhang)
    prefix.foreach(buffer.append("--outFileNamePrefix", _))
    buffer
  }
}

/** Defaults that are used when generating genomic reference data with `STAR` */
object StarGenerateGenome {

  /** The default cores to use. */
  val DefaultCores = Cores(4)

  /** The default memory to use. */
  val DefaultMemory = Memory("8")
}
