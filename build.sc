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

trait CommonModule extends ScalaModule {
  def scalaVersion = "2.12.2"

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

trait ScalaTest extends TestModule {
  override def ivyDeps = Agg(ivy"org.scalatest::scalatest::3.0.7".excludeOrg(organizations="org.junit"))
  override def testFrameworks: Target[Seq[String]] = Seq("org.scalatest.tools.Framework")
}

object commons extends CommonModule {

  def gitHash: String = %%("git", "rev-parse", "--short", "HEAD")(pwd).out.string.trim

  override def scalacOptions: Target[Seq[String]] = Seq("-target:jvm-1.8", "-deprecation")

  override def ivyDeps = Agg(
    ivy"com.fulcrumgenomics::commons::$fgbioCommonsVersion",
    ivy"com.fulcrumgenomics::fgbio::$fgbioVersion".excludeOrg(organizations=excludeOrg: _*),
    ivy"org.reflections:reflections:0.9.11",
    ivy"org.slf4j:slf4j-nop:1.7.6"  // For logging silence: https://www.slf4j.org/codes.html#StaticLoggerBinder
  )

  // def manifest = T { super.manifest().add(ImplementationVersion.toString -> s"cvbioVersion-$gitHash-SNAPSHOT") }

  override def assemblyRules = Seq(
    ExcludePattern(".*\\.git.*"),
    ExcludePattern(".*chromosome-mappings/README.md")
  )

  object test extends Tests with ScalaTest { override def moduleDeps = Seq(commons) }
}

object pipelines extends CommonModule {
  override def ivyDeps: Target[Loose.Agg[Dep]] = super.ivyDeps() ++ Agg(
    ivy"com.fulcrumgenomics::dagr-core::$dagrCoreVersion",
    ivy"com.fulcrumgenomics::dagr-tasks::$dagrCoreVersion"
  )

  override def moduleDeps = Seq(commons)

  def localJar = T { super.localJar(assembly(), jarName="cvbio-pipelines.jar") }

  object test extends Tests with ScalaTest { override def moduleDeps = Seq(pipelines, commons.test) }
}

object tools extends CommonModule {

  override def ivyDeps: Target[Loose.Agg[Dep]] = super.ivyDeps() ++ Agg(
    ivy"org.apache.httpcomponents:httpclient:4.5.8"
  )

  override def moduleDeps = Seq(commons)

  def localJar = T { super.localJar(assembly(), jarName="cvbio.jar") }

  object test extends Tests with ScalaTest { override def moduleDeps = Seq(tools, commons.test) }
}
