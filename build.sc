import mill.contrib.scoverage.ScoverageModule
import $ivy.`com.lihaoyi::mill-contrib-scoverage:0.5.0`
import ammonite.ops._
import coursier.maven.MavenRepository
import mill._
import mill.api.Loose
import mill.define.Target
import mill.modules.Assembly.Rule.ExcludePattern
import mill.scalalib._

private val cvbioVersion        = "0.0.5"
private val dagrCoreVersion     = "0.6.0-46843f8-SNAPSHOT"
private val fgbioCommonsVersion = "0.8.0-3087de3-SNAPSHOT"
private val fgbioVersion        = "0.9.0-f2cfac4-SNAPSHOT"

private val excludeOrg = Seq("com.google.cloud.genomics", "gov.nih.nlm.ncbi", "org.apache.ant",  "org.testng")

/** The common module mixin for all of our projects. */
trait CommonModule extends ScalaModule with ScoverageModule {
  def scalaVersion     = "2.12.2"
  def scoverageVersion = "1.3.1"

  override def repositories: Seq[coursier.Repository] = super.repositories ++ Seq(
    MavenRepository("https://oss.sonatype.org/content/repositories/public"),
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
    MavenRepository("https://jcenter.bintray.com/")
  )

  def localJar(assembly: PathRef, jarName: String): Unit = {
    mkdir(pwd / 'jars)
    cp.over(assembly.path, pwd / 'jars / jarName)
  }
}

/** The ScalaTest settings. */
trait ScalaTest extends TestModule {
  override def ivyDeps = Agg(ivy"org.scalatest::scalatest::3.0.7".excludeOrg(organizations="org.junit"))
  override def testFrameworks: Target[Seq[String]] = Seq("org.scalatest.tools.Framework")
}

/** The commons project. */
object commons extends CommonModule {

  /** The current short Git hash. */
  def gitHash: String = %%("git", "rev-parse", "--short", "HEAD")(pwd).out.string.trim

  /** Scala compiler options. */
  override def scalacOptions: Target[Seq[String]] = Seq("-target:jvm-1.8", "-deprecation", "-feature")

  // def manifest = T { super.manifest().add(ImplementationVersion.toString -> s"cvbioVersion-$gitHash-SNAPSHOT") }

  /** Exclude these resource paths when building subsequent JARs with the commons project. */
  override def assemblyRules = Seq(
    ExcludePattern(".*\\.git.*"),
    ExcludePattern(".*chromosome-mappings/README.md")
  )

  /** Ivy dependencies. */
  override def ivyDeps = Agg(
    ivy"com.fulcrumgenomics::commons::$fgbioCommonsVersion",
    ivy"com.fulcrumgenomics::fgbio::$fgbioVersion".excludeOrg(organizations=excludeOrg: _*),
    ivy"org.reflections:reflections:0.9.11",
    ivy"org.slf4j:slf4j-nop:1.7.6"  // For logging silence: https://www.slf4j.org/codes.html#StaticLoggerBinder
  )

  /** Test the tools commons project. */
  object test extends Tests with ScalaTest with ScoverageTests
}

/** The pipelines project. */
object pipelines extends CommonModule {

  /** Ivy dependencies. */
  override def ivyDeps: Target[Loose.Agg[Dep]] = super.ivyDeps() ++ Agg(
    ivy"com.fulcrumgenomics::dagr-core::$dagrCoreVersion",
    ivy"com.fulcrumgenomics::dagr-tasks::$dagrCoreVersion"
  )

  /** Module dependencies. */
  override def moduleDeps = Seq(commons)

  /** Build a JAR file from the pipelines project. */
  def localJar = T { super.localJar(assembly(), jarName = "cvbio-pipelines.jar") }

  /** Test the tools pipelines project. */
  object test extends Tests with ScalaTest with ScoverageTests {
    override def moduleDeps: Seq[JavaModule] =  super.moduleDeps ++ Seq(commons.test)
  }
}

/** The tools project. */
object tools extends CommonModule {

  /** Ivy dependencies. */
  override def ivyDeps: Target[Loose.Agg[Dep]] = super.ivyDeps() ++ Agg(
    ivy"org.apache.httpcomponents:httpclient:4.5.8"
  )

  /** Module dependencies. */
  override def moduleDeps = Seq(commons)

  /** Build a JAR file from the tools project. */
  def localJar = T { super.localJar(assembly(), jarName = "cvbio.jar") }

  /** Test the tools project. */
  object test extends Tests with ScalaTest with ScoverageTests {
    override def moduleDeps: Seq[JavaModule] =  super.moduleDeps ++ Seq(commons.test)
  }
}
