package io.cvbio.commons.environ

import io.cvbio.commons.effectful.{CommandLineTool, Modular, Versioned}
import spray.json._

import scala.util.{Failure, Success}

object Conda extends CommandLineTool with Versioned with Modular {

  /** The name of the executable. */
  val executable: String = "conda"

  /** The command to use for discovering if a package is installed or not. */
  override def testModuleCommand(module: String): Seq[String] = Seq(executable, "list", "--full-name", "--json", module)

  /** Returns true if the tested module exist with the tested executable. */
  override def moduleAvailable(module: String): Boolean = {
    import io.cvbio.commons.environ.Conda.PackageInfoJsonProtocol._
    if (Conda.available) {
      CommandLineTool.execCommand(testModuleCommand(module), Some(logger)) match {
        case Success(value) => value.mkString.parseJson.convertTo[Seq[PackageInfo]].exists(_.name == module)
        case Failure(_: CommandLineTool.ToolException) => false
        case Failure(e: Throwable) => throw e
      }
    } else { false }
  }

  /** A data class representing the information of a conda package. */
  case class PackageInfo(
    base_url: String,
    build_number: Int,
    build_string: String,
    channel: String,
    dist_name: String,
    name: String,
    platform: String,
    version: String
 )

  /** A (de)serialization protocol for [[PackageInfo]] classes. */
  object PackageInfoJsonProtocol extends DefaultJsonProtocol {

    /** How to convert JSON to [[PackageInfo]] and back to to JSON again. */
    implicit val packageInfoFormat: RootJsonFormat[PackageInfo] = jsonFormat8(PackageInfo)
  }
}
