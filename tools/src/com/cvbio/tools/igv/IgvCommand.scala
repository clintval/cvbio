package com.cvbio.tools.igv

import com.cvbio.commons.CommonsDef.{DirPath, FilePath}
import com.cvbio.commons.StringUtil
import htsjdk.samtools.util.Interval

// TODO: Redesign the underlying API. A lot can be handled by traits, although I think I
//       need reflection or move the build to 2.13 so I can access fields of case classes.

/** An IGV command that all commands should inherit from. */
private[igv] sealed trait IgvCommand {
  def params: Seq[Any] = Seq.empty
  def simpleName: String = StringUtil.uncapitilize(this.getClass.getSimpleName.split("\\$").last)
  override def toString: String = (Seq(simpleName) ++ params.map(_.toString)).mkString(" ")
}

/** For all IGV commands which take an optional track name. */
private[igv] sealed abstract class OptionalTrackName(trackName: Option[String]) extends IgvCommand {
  override def toString: String = (Seq(simpleName) ++ trackName).mkString(" ")
}

/** Writes "echo" back to the response. Primarily used for testing. */
private[igv] case object Echo extends IgvCommand

/** Exits by completely closing the IGV application. */
private[igv] case object Exit extends IgvCommand

/** Creates a new session. Unloads all tracks except the default genome annotations. */
private[igv] case object New extends  IgvCommand

/** Collapses a given <trackName>. A <trackName> is optional, and if it is not supplied all tracks are collapsed. */
private[igv] case class Collapse(trackName: Option[String]) extends OptionalTrackName(trackName = trackName)

/** Expands a given <trackName>. A <trackName> is optional, and if it is not supplied all tracks are expanded. */
private[igv] case class Expand(trackName: Option[String]) extends OptionalTrackName(trackName = trackName)

/** Selects a genome by id. */
private[igv] case class Genome(genome: String) extends IgvCommand

/** Companion object to [[Genome]] for other  */
private[igv] object Genome {

  /** Selects a genome (or indexed fasta) from the supplied path. */
  def apply(genome: FilePath): Genome = new Genome(genome.toString)
}

/** Scrolls to a single locus or a space-delimited list of loci. If a list is provided, these loci will be displayed in
  * a split screen view. Use any syntax that is valid in the IGV search box. Inputting "all" scrolls to a whole genome
  * view.
  */
private[igv] case class Goto(locus: Seq[String]) extends IgvCommand {
  override val params: Seq[Any] = Seq(locus)
  override def toString: String = {
    (Seq(simpleName) ++ locus.map(_.toString)).mkString(" ")
  }
}

/** Companion object to [[Goto]]. */
private[igv] object Goto {
  def apply(interval: Interval): Goto = {
    new Goto(Seq(interval.getContig + ":" + interval.getStart + "-" + interval.getEnd)
  }
}

/** Loads data or session files. Specify a comma-delimited list of full paths or URLs. */
private[igv] case class Load(file: String) extends IgvCommand { override val params: Seq[Any] = Seq(file) }

/** Companion object to [[Load]]. */
private[igv] object Load {
  def apply(file: FilePath): Load = Load(file.toString)
  def apply(file: Seq[FilePath]): Load = Load(file.mkString(","))
}


/** Defines a region of interest bounded by the coordinates on a reference sequence. */
private[igv] case class Region(interval: Interval) extends IgvCommand {
  override val params: Seq[Any] = Seq(interval)
  override def toString: String = Seq(simpleName, interval.getContig, interval.getStart, interval.getEnd).mkString(" ")
}

/** Sets the number of vertical pixels (height) of each panel to include in image. Images created from a port command
  * or batch script are not limited to the data visible on the screen. Stated another way, images can include the entire
  * panel not just the portion visible in the scrollable screen area. The default value for this setting is 1000,
  * increase it to see more data, decrease it to create smaller images.
  */
private[igv] case class MaxPanelHeight(height: Int) extends IgvCommand

/** Set to a log-scale or not. */
private[igv] case class SetLogScale(underlying: Boolean) extends AnyRef with IgvCommand

/** Sets a delay (sleep) time in milliseconds.  The sleep interval is invoked between successive commands. */
private[igv] case class SetSleepInterval(ms: Double) extends IgvCommand

/** Sets the directory in which to write images. */
private[igv] case class SnapshotDirectory(dir: DirPath) extends IgvCommand

/** Saves a snapshot of the IGV window to an image file. If filename is omitted, writes a PNG file with a filename
  * generated based on the locus. If filename is specified, the filename extension determines the image file format,
  * which must be .png, .jpg, or .svg.
  */
private[igv] case class Snapshot(path: FilePath) extends IgvCommand { override val params: Seq[Any] = Seq(path) }

/** Sorts an alignment track by the specified option. Recognized values for the option parameter are: base, position,
  * strand, quality, sample, readGroup, AMPLIFICATION, DELETION, EXPRESSION, SCORE, and MUTATION_COUNT. The locus
  * option can define a single position, or a range. If absent sorting will be perfomed based on the region in view,
  * or the center position of the region in view, depending on the option.
  */
private[igv] case class Sort(option: String, locus: String) extends IgvCommand

/** Squish a given trackName. trackName is optional, and if it is not supplied all annotation tracks are squished. */
private[igv] case class Squish(trackName: String) extends IgvCommand

/** Set the display mode for an alignment track to "View as pairs". A <trackName> is optional. */
private[igv] case class ViewAsPairs(trackName: Option[String]) extends OptionalTrackName(trackName = trackName)

/** Temporarily set the preference named <key> to the specified <value>. This preference only lasts until IGV is
  * shut-down.
  */
private[igv] case class Preference(key: String, value: String) extends IgvCommand
