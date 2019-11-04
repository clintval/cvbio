package com.cvbio.tools.igv

import com.cvbio.commons.CommonsDef._
import com.cvbio.tools.cmdline.{ClpGroups, CvBioTool}
import com.cvbio.tools.igv.Igv.{DefaultHost, DefaultPort, DefaultMemory, Executable}
import com.fulcrumgenomics.sopt._

import scala.collection.mutable.ListBuffer

@clp(
  description =
    """
      |Take control of your IGV session from end-to-end.
      |
      |### IGV Startup
      |
      |There are three supported ways to initialize IGV:
      |
      |  - Let this tool connect to an already-running IGV session
      |  - Supply an IGV JAR file path and let this tool run it
      |  - Let this tool find an `igv` executable on the system PATH and run it
      |
      |This tool will always attempt to connect to a running IGV application before attempting to start a new instance
      |of IGV. Provide a path to an IGV JAR file if no IGV applications are currently running. If no IGV JAR file path
      |is set, and there are no running instances of IGV, then this tool will attempt to fnd `igv` on the system PATH
      |and execute the application.
      |
      |You can shutdown IGV on exit with the `--close-on-exit` option. This will work regardless of how this tool
      |initially connected to IGV and is handy for tearing down the application after your investigation is concluded.
      |
      |### Controlling IGV
      |
      |If no inputs are provided, then no new sessions will be created. Loci, for now, will result in a split-window
      |view.
      |
      |## References and Prior Art
      |
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
  @arg(flag = 'm', doc = "The memory (in gigabytes) given to the JVM, if we are to initialize IGV.") val memory: Int = DefaultMemory,
  @arg(flag = 'H', doc = "The host the IGV server is running on.") val host: String = DefaultHost,
  @arg(flag = 'p', doc = "The port to the IGV server.") val port: Int = DefaultPort,
  @arg(flag = 'x', doc = "Close the IGV application after execution") val closeOnExit: Boolean = false,
) extends CvBioTool {

  /** Run the tool [[IgvBoss]]. */
  override def execute: Unit = {
    val commands = new ListBuffer[IgvCommand]()

    genome.foreach(g => commands += Genome(g))

    if (input.nonEmpty) { commands += New += Load(input) }
    if (loci.nonEmpty)  { commands += Goto(loci) }

    igv.exec(commands)
  }

  /** Connect to IGV if it is available, else initialize and then connect. */
  private lazy val igv: Igv = {
    if (Igv.available(host, port)) { new Igv(host, port) } else {
      jar match {
        case Some(_jar) => Igv(_jar, memory, port, closeOnExit)
        case None       => Igv(Executable, port, closeOnExit)
      }
    }
  }
}
