package com.cvbio.tools.igv

import com.cvbio.commons.CommonsDef.{DirPath, FilePath}
import com.cvbio.commons.StringUtil
import htsjdk.samtools.util.Interval

// TODO: Clean up entire file! Whatta mess.
sealed trait IgvCommand {
  def params: Seq[Any] = Seq.empty
  def simpleName: String = StringUtil.uncapitilize(this.getClass.getSimpleName.split("\\$").last)
  override def toString: String = (Seq(simpleName) ++ params.map(_.toString)).mkString(" ")
}

// TODO: Use reflection to graph key-value field values
case object Echo extends IgvCommand
case object Exit extends IgvCommand

/** Create a new session. Unloads all tracks except the default genome annotations. */
case object New extends  IgvCommand

/** Collapses a given trackName. <trackName> is optional, however, and if it is not supplied all tracks are collapsed. */
case class Collapse(trackName: Option[String]) extends IgvCommand {
  override val params: Seq[Any] = Seq(trackName)
  override def toString: String = (Seq(simpleName) ++ trackName).mkString(" ")
}
case class Expand(trackName: String) extends IgvCommand { override val params: Seq[Any] = Seq(trackName) }
case class Genome(genomeIdOrPath: String) extends IgvCommand { override val params: Seq[Any] = Seq(genomeIdOrPath) }
case class Goto(locus: Seq[Locus]) extends IgvCommand { // TODO: Wrong base type in constructor...
  override val params: Seq[Any] = Seq(locus)
  override def toString: String = {
    (Seq(simpleName) ++ locus.map(_.toString)).mkString(" ")
  }
}
object Goto {
  def apply(locus: Locus): Goto = new Goto(Seq(locus))
  def apply(interval: Interval): Goto = Goto.apply(Locus(interval.getContig, Some(interval.getStart), Some(interval.getEnd)))
}

/** Loads data or session files. Specify a comma-delimited list of full paths or URLs. */
case class Load(file: String) extends IgvCommand { override val params: Seq[Any] = Seq(file) }
object Load {
  def apply(file: FilePath): Load = Load(file.toString)
  def apply(file: Seq[FilePath]): Load = Load(file.mkString(","))
}
case class Region(interval: Interval) extends IgvCommand {
  override val params: Seq[Any] = Seq(interval)
  override def toString: String = Seq(simpleName, interval.getContig, interval.getStart, interval.getEnd).mkString(" ")
}
case class MaxPanelHeight(height: Int) extends IgvCommand { override val params: Seq[Any] = Seq(height) }
case class SetLogScale(underlying: Boolean) extends AnyRef with IgvCommand { override val params: Seq[Any] = Seq(underlying) }
case class SetSleepInterval(ms: Double) extends IgvCommand { override val params: Seq[Any] = Seq(ms) }
case class SnapshotDirectory(dir: DirPath) extends IgvCommand { override val params: Seq[Any] = Seq(dir) }
case class Snapshot(path: FilePath) extends IgvCommand { override val params: Seq[Any] = Seq(path) }
case class Sort(option: String, locus: Locus) extends IgvCommand { override val params: Seq[Any] = Seq(option, locus) }
case class Squish(trackName: String) extends IgvCommand { override val params: Seq[Any] = Seq(trackName) }
case class ViewAsPairs(trackName: String) extends IgvCommand { override val params: Seq[Any] = Seq(trackName) }
case class Preference(key: String, value: String) extends IgvCommand { override val params: Seq[Any] = Seq(key, value) }

/** A 1-based genomic span with optional start and end positions. */ // TODO: A locus in IGV-terms is not just this.
case class Locus(contig: String, start: Option[Int], end: Option[Int]) {
  override def toString: String = {
    (start, end) match {
      case (Some(s), Some(e)) => s"$contig:$s-$e"
      case (Some(s), None)    => s"$contig:$s"
      case (None, Some(e))    => s"$contig:$e"
      case (None, None)       => contig
    }
  }
}
