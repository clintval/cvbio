package com.cvbio.pipeline.tasks.star

import com.cvbio.commons.CommonsDef._
import com.fulcrumgenomics.FgBioDef.FgBioEnum
import dagr.core.execsystem.{Cores, Memory}
import dagr.core.tasksystem.{FixedResources, ProcessTask}
import enumeratum.EnumEntry

import scala.collection.mutable.ListBuffer

/** Align paired-end reads from FASTQ files with the `STAR` aligner.
  *
  * The `STAR` aligner requires that if any of the following tags are defined, then the tag `id` must also be defined:
  *
  * - `sampleName`
  * - `library`
  * - `platform`
  * - `platformUnit`
  * */
class StarAlign(
  read1: PathToFastq,
  read2: Option[PathToFastq],
  genomeDir: DirPath,
  prefix: Option[PathPrefix] = None,
  twoPass: Option[StarAlign.TwoPassMode] = None,
  id: Option[String] = None,
  sampleName: Option[String] = None,
  library: Option[String] = None,
  platform: Option[String] = None,
  platformUnit: Option[String] = None,
  cores: Cores = StarAlign.DefaultCores
) extends ProcessTask
  with FixedResources {

  require(
    !(id.isEmpty && Seq(sampleName, library, platform, platformUnit).exists(_.nonEmpty)),
    "If any of `sampleName`, `library`, `platform`, or `platformUnit` are defined, `id` must also be defined."
  )
  requires(cores, StarAlign.DefaultMemory)

  /** The command line arguments. */
  override def args: Seq[Any] = {
    val buffer = ListBuffer[Any]()

    buffer.append(Star.findStar)
    buffer.append("--runThreadN", resources.cores.toInt)
    buffer.append("--runMode", "alignReads")
    buffer.append(Seq("--readFilesIn") ++ readFiles: _*)
    buffer.append("--genomeDir", genomeDir)
    prefix.foreach(buffer.append("--outFileNamePrefix", _))
    twoPass.foreach(buffer.append("--twoPassMode", _))
    id.foreach(_ => buffer.append(Seq("--outSAMattrRGline") ++ readGroupLine: _*))
    buffer.append("--outSAMattributes", "All")
    buffer.append("--outSAMmode", "Full")
    buffer.append("--outSAMprimaryFlag", "OneBestScore")
    buffer.append("--outSAMtype", "BAM", "SortedByCoordinate")
    buffer.append("--outSAMunmapped", "Within", "KeepPairs")

    buffer
  }

  /** Format the FASTQ file paths argument. */
  private[star] def readFiles: Seq[String] = {
    val files = read2 match {
      case Some(_read2) => Seq(read1, _read2)
      case None         => Seq(read1)
    }
    files.map(_.toString)
  }

  /** Format the read group tags into a read group line.
    *
    * `STAR` requires that the first tag be the "ID" tag and that all additional tags follow, separated by whitespace.
    * A developer caveat is that `STAR` will not parse single-quoted argument lists correctly and the resulting read
    * group will be formatted in an invalid way within the SAM file. Instead, the argument groups must exist "naked" on
    * the command line and only be encapsulated in double-quotes when there is whitespace within a single tag and value.
    *
    * For example, this will silently create a malformed BAM:
    *
    * {{{
    *   "--outSAMattrRGline 'ID:1 SM:sampleName LB:1 PL:ILLUMINA PU:HA3J2JDF'"
    * }}}
    *
    * However, this will work appropriately:
    *
    * {{{
    *   "--outSAMattrRGline ID:1 SM:sampleName LB:1 PL:ILLUMINA PU:HA3J2JDF"
    * }}}
    *
    * Similarly, this will work when there are spaces in the tag's value:
    *
    * {{{
    *   "--outSAMattrRGline ID:1 SM:sampleName LB:1 PL:ILLUMINA PU:HA3J2JDF "DS:My favorite sample!""
    * }}}
    * */
  private[star] def readGroupLine: Seq[String] = {

    /** Make the tag value string for the `STAR` command line. */
    def makeTag(tag: String, value:Option[String]): Option[String] = value.map(tag + ":" + _)

    Seq(
      makeTag("ID", id),
      makeTag("SM", sampleName),
      makeTag("LB", library),
      makeTag("PL", platform),
      makeTag("PU", platformUnit)
    ).flatten
  }
}

/** Defaults that are used when aligning with `STAR`. */
object StarAlign {

  /** The filename suffix `STAR` uses for the coordinate sorted output BAM. */
  val AlignedCoordinateSortedSuffix: FilenameSuffix = "Aligned.sortedByCoord.out.bam"

  /** The default cores to use. */
  val DefaultCores = Cores(6)

  /** The default memory to use. */
  val DefaultMemory = Memory("24G")

  /** Trait that all enumeration values of type [[ChimeraOutputType]] should extend. */
  sealed trait ChimeraOutputType extends EnumEntry

  /** Contains enumerations of chimera output types. */
  object ChimeraOutputType extends FgBioEnum[ChimeraOutputType] {

    def values: scala.collection.immutable.IndexedSeq[ChimeraOutputType] = findValues

    /** The value when [[ChimeraOutputType]] is the file "Chimeric.out.junction". */
    case object Junctions extends ChimeraOutputType

    /** The value when [[ChimeraOutputType]] is a separate SAM file. */
    case object SeparateSAMold extends ChimeraOutputType

    /** The value when [[ChimeraOutputType]] is a within the main aligned BAM. */
    case object WithinBam extends ChimeraOutputType
  }

  /** Trait that all enumeration values of type [[ChimeraOutputClip]] should extend. */
  sealed trait ChimeraOutputClip extends EnumEntry

  /** Contains enumerations of chimera output clipping strategies. */
  object ChimeraOutputClip extends FgBioEnum[ChimeraOutputClip] {

    def values: scala.collection.immutable.IndexedSeq[ChimeraOutputClip] = findValues

    /** The value when [[ChimeraOutputClip]] is a hard clip. */
    case object HardClip extends ChimeraOutputClip

    /** The value when [[ChimeraOutputClip]] is a soft clip. */
    case object SoftClip extends ChimeraOutputClip
  }

  /** Trait that all enumeration values of type [[TwoPassMode]] should extend. */
  sealed trait TwoPassMode extends EnumEntry

  /** Contains enumerations of two pass strategies. */
  object TwoPassMode extends FgBioEnum[TwoPassMode] {

    def values: scala.collection.immutable.IndexedSeq[TwoPassMode] = findValues

    /** The value when [[TwoPassMode]] is no two pass. */
    case object None extends TwoPassMode

    /** The value when [[TwoPassMode]] is a basic two pass mode. */
    case object Basic extends TwoPassMode
  }
}
