package io.cvbio.tools.igv

import io.cvbio.commons.CommonsDef._
import io.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import io.cvbio.tools.igv.Igv.{DefaultHost, DefaultMemory, DefaultPort, Executable}
import io.cvbio.tools.igv.IgvPreferences._
import com.fulcrumgenomics.sopt._
import io.cvbio.commons.ConfigurationUtil

import scala.collection.mutable.ListBuffer

@clp(
  description =
    """
      |Take control of your IGV session from end-to-end.
      |
      |If no inputs are provided, then no new sessions will be created. Adding multiple IGV-valid locus identifiers will
      |result in a split-window view. You must have already configured your IGV application to allow HTTPS connections
      |over a port. Enable remote control through the Advanced Tab of the Preferences Window in IGV.
      |
      |## IGV Startup
      |
      |IGV will be initialized using the ordered logic:
      |
      |  1. Let this tool connect to an already-running IGV session
      |  2. Supply an IGV JAR file path and let this tool run the JAR
      |  3. If you're on MacOS and have the Mac Application installed, IgvBoss will run it
      |  4. Finally, IgvBoss will attempt to find the `igv` executable on the system path and run it
      |
      |IgvBoss will always attempt to connect to a running IGV application before attempting to start a new instance of
      |IGV. Provide a path to an IGV JAR file if no IGV applications are currently running. If no IGV JAR file path
      |is set, and there are no running instances of IGV, then IgvBoss will attempt to fnd a locally installed version
      |of IGV and run it. If you are executing IgvBoss on a MacOS system, then IgvBoss will first look for an installed
      |IGV Mac application. If one cannot be found, or you're on a different operating system, then IgvBoss will search
      |for and `igv` executable on the system path to execute.
      |
      |## IGV Shutdown
      |
      |You can shutdown IGV when IgvBoss exits with the `--close-on-exit` option. This will work regardless of how
      |IgvBoss initially connected to IGV. This feature is handy for tearing down the application after your
      |investigation is concluded.
      |
      |## References and Prior Art
      |
      |  - https://github.com/igvteam/igv/blob/master/src/main/resources/org/broad/igv/prefs/preferences.tab
      |  - https://software.broadinstitute.org/software/igv/PortCommands
      |  - https://github.com/stevekm/IGV-snapshot-automator
    """,
  group = ClpGroups.Util
) class IgvBoss(
  @arg(flag = 'i', doc = "Input files to display.", minElements = 0) val input: Seq[FilePath] = Seq.empty,
  @arg(flag = 'g', doc = "The genome to use (path, string, id).") genome: Option[String] = None,
  @arg(flag = 'l', doc = "The loci to visit. (e.g. \"all\", \"chr1:23-99\", \"TP53\").", minElements = 0) val loci: Seq[String] = Seq.empty,
  // @arg(flag = 'o', doc = "Output path prefix to the rendered images.") val output: Option[PathPrefix] = None,
  // @arg(flag = 'f', doc = "The output snapshot format") val format: OutputFormat = OutputFormat.Svg,
  @arg(flag = 'j', doc = "The IGV Jar file, if we are to initialize IGV.") val jar: Option[FilePath] = None,
  @arg(flag = 'm', doc = "The heap size (Gb) given to the JVM, if we initialize.") val memory: Int = DefaultMemory,
  @arg(flag = 'H', doc = "The host the IGV server is running on.") val host: String = DefaultHost,
  @arg(flag = 'p', doc = "The port to the IGV server.") val port: Int = DefaultPort,
  @arg(doc = "Downsample reads or not.") val downsample: Option[Boolean] = None,
  @arg(doc = "Minimum base quality to shade.") val baseQualityMinimum: Option[Int] = None,
  @arg(doc = "Maximum base quality to shade.") val baseQualityMaximum: Option[Int] = None,
  @arg(flag = 'x', doc = "Close the IGV application after execution.") val closeOnExit: Boolean = false,
) extends CvBioTool {

  /** Run the tool [[IgvBoss]]. */
  override def execute: Unit = {
    val commands = new ListBuffer[IgvCommand]()

    genome.foreach(g => commands += Genome(g))

    downsample.foreach(value => commands += Preference(Sam.DownsampleReads, value))
    baseQualityMinimum.foreach(value => commands += Preference(Sam.MinimumBaseQualityToShade, value))
    baseQualityMaximum.foreach(value => commands += Preference(Sam.MaximumBaseQualityToShade, value))

    if (input.nonEmpty) { commands += New += Load(input) }
    if (loci.nonEmpty)  { commands += Goto(loci) }

    igv.exec(commands)
  }

  /** Connect to IGV if it is available, else initialize and then connect. */
  private lazy val igv: Igv = {
    if (Igv.available(host, port)) { new Igv(host, port) } else {
      jar match {
        case Some(_jar) => Igv(_jar, memory, port, closeOnExit)
        case None       => {
          ConfigurationUtil.findMacApplication(Executable.toUpperCase) match {
            case Some(macApp) => Igv(macApp, memory, port, closeOnExit)
            case None         => Igv(Executable, port, closeOnExit)
          }
        }
      }
    }
  }
}
