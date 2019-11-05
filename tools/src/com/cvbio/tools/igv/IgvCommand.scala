package com.cvbio.tools.igv

import com.cvbio.commons.CommonsDef.{DirPath, FilePath}
import com.cvbio.commons.StringUtil
import htsjdk.samtools.util.Locatable

/** An IGV command that all commands inherit from. */
sealed trait IgvCommand {
  def simpleName: String = StringUtil.uncapitilize(getClass.getSimpleName.split("\\$").last)
  override def toString: String = simpleName
}

/** For all IGV commands which take an optional track name. */
trait WithOptionalTrackName extends IgvCommand {
  val trackName: Option[String]
  override def toString: String = (Seq(simpleName) ++ trackName).mkString(" ")
}

/** Collapses a given <trackName>. A <trackName> is optional, and if it is not supplied all tracks are collapsed. */
case class Collapse(trackName: Option[String] = None) extends WithOptionalTrackName

/** Writes "echo" back to the response. Primarily used for testing. */
case object Echo extends IgvCommand

/** Exits by completely closing the IGV application. */
case object Exit extends IgvCommand

/** Expands a given <trackName>. A <trackName> is optional, and if it is not supplied all tracks are expanded. */
case class Expand(trackName: Option[String] = None) extends WithOptionalTrackName

/** Selects a genome by ID. */
case class Genome(genome: String) extends IgvCommand {
  override def toString: String = Seq(simpleName, genome).mkString(" ")
}

/** Companion object to [[Genome]] for other  */
object Genome {

  /** Selects a genome (or indexed fasta) from the supplied path. */
  def apply(genome: FilePath): Genome = new Genome(genome.toAbsolutePath.toString)
}

/** Scrolls to a single locus or sequence of loci. If a sequence is provided, these loci will be displayed in a split
  * screen view. Use any syntax that is valid in the IGV search box. Inputting "all" scrolls to a whole genome view.
  */
case class Goto(locus: Seq[String]) extends IgvCommand {
  override def toString: String = (simpleName +: locus).mkString(" ")
}

/** Companion object to [[Goto]]. */
 object Goto {

  /** Go to a single locus. */
  def apply(locus: String): Goto = Goto(Seq(locus))

  /** Go to a locatable. */
  def apply[T <: Locatable](locatable: T): Goto = {
    new Goto(Seq(locatable.getContig + ":" + locatable.getStart + "-" + locatable.getEnd))
  }
}

/** Loads data or session files. Specify a comma-delimited list of full paths or URLs. */
case class Load(file: String) extends IgvCommand {
  override def toString: String = Seq(simpleName, file).mkString(" ")
}

/** Companion object to [[Load]]. */
object Load {
  def apply(file: FilePath): Load      = Load(file.toString)
  def apply(file: Seq[FilePath]): Load = Load(file.mkString(","))
}


/** Defines a region of interest bounded by the coordinates on a reference sequence. */
case class Region[T <: Locatable](locatable: T) extends IgvCommand {
  override def toString: String = Seq(simpleName, locatable.getContig, locatable.getStart, locatable.getEnd).mkString(" ")
}

/** Sets the number of vertical pixels (height) of each panel to include in image. Images created from a port command
  * or batch script are not limited to the data visible on the screen. Stated another way, images can include the entire
  * panel not just the portion visible in the scrollable screen area. The default value for this setting is 1000,
  * increase it to see more data, decrease it to create smaller images.
  */
case class MaxPanelHeight(height: Int) extends IgvCommand {
  override def toString: String = Seq(simpleName, height).mkString(" ")
}

/** Creates a new session. Unloads all tracks except the default genome annotations. */
case object New extends IgvCommand

/** Set to a log-scale or not. */
case class SetLogScale(underlying: Boolean) extends AnyRef with IgvCommand {
  override def toString: String = Seq(simpleName, underlying).mkString(" ")
}

/** Sets a delay (sleep) time in milliseconds.  The sleep interval is invoked between successive commands. */
case class SetSleepInterval(ms: Double) extends IgvCommand {
  override def toString: String = Seq(simpleName, ms).mkString(" ")
}

/** Sets the directory in which to write images. */
case class SnapshotDirectory(dir: DirPath) extends IgvCommand {
  override def toString: String = Seq(simpleName, dir).mkString(" ")
}

/** Saves a snapshot of the IGV window to an image file. If filename is omitted, writes a PNG file with a filename
  * generated based on the locus. If filename is specified, the filename extension determines the image file format,
  * which must be .png, .jpg, or .svg.
  */
case class Snapshot(path: FilePath) extends IgvCommand {
  override def toString: String = Seq(simpleName, path).mkString(" ")
}

/** Sorts an alignment track by the specified option. Recognized values for the option parameter are: base, position,
  * strand, quality, sample, readGroup, AMPLIFICATION, DELETION, EXPRESSION, SCORE, and MUTATION_COUNT. The locus
  * option can define a single position, or a range. If absent sorting will be performed based on the region in view,
  * or the center position of the region in view, depending on the option.
  */
case class Sort(option: String, locus: String) extends IgvCommand {
  override def toString: String = Seq(simpleName, option, locus).mkString(" ")
}

/** Squish a given <trackName>. <trackName> is optional, and if it is not supplied all annotation tracks are squished. */
case class Squish(trackName: Option[String] = None) extends WithOptionalTrackName

/** Set the display mode for an alignment track to "View as pairs". A <trackName> is optional. */
case class ViewAsPairs(trackName: Option[String] = None) extends WithOptionalTrackName

/** Temporarily set the preference named <key> to the specified <value>. This preference only lasts until IGV is
  * shut-down.
  */
case class Preference(key: String, value: String) extends IgvCommand {
  override def toString: String = Seq(simpleName, key, value).mkString(" ")
}

/** Companion object for [[Preference]]. */
object Preference {

  /** Build a preference command from anything. */
  def apply(key: String, value: Any): Preference = new Preference(key, value.toString)
}
