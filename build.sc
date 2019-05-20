import ammonite.ops._
import coursier.maven.MavenRepository
import mill._
import mill.modules.Assembly.Rule.ExcludePattern
import mill.scalalib._

private val dagrCoreVersion     = "0.6.0-e114e03-SNAPSHOT"
private val fgbioCommonsVersion = "0.8.0-6d2f0a3-SNAPSHOT"
private val fgbioVersion        = "0.8.1"

private val excludeOrg = Seq("com.google.cloud.genomics", "gov.nih.nlm.ncbi", "org.apache.ant",  "org.testng")

trait CommonModule extends SbtModule {
  def scalaVersion = "2.12.2"

  override def repositories: Seq[coursier.Repository] = super.repositories ++ Seq(
    MavenRepository("https://oss.sonatype.org/content/repositories/public"),
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
    MavenRepository("https://jcenter.bintray.com/")
  )

  def deployLocal(assembly: PathRef, jarName: String): Unit = {
    mkdir(pwd / 'jars)

    cp.over(assembly.path, pwd / 'jars / jarName)
  }
}

trait ScalaTest extends TestModule {
  override def ivyDeps = Agg(ivy"org.scalatest::scalatest::3.0.7".excludeOrg("org.junit"))
  override def testFrameworks = Seq("org.scalatest.tools.Framework")
}

object commons extends CommonModule {

  override def ivyDeps = Agg(
    ivy"org.slf4j:slf4j-nop:1.7.6",  // For logging silence: https://www.slf4j.org/codes.html#StaticLoggerBinder
    ivy"org.apache.httpcomponents:httpclient:4.5.8",
    ivy"com.fulcrumgenomics::commons::$fgbioCommonsVersion",
    ivy"com.fulcrumgenomics::fgbio::$fgbioVersion".excludeOrg(excludeOrg: _*),
    ivy"org.reflections:reflections:0.9.11"
  )

  override def assemblyRules = Seq(
    ExcludePattern(".*\\.git.*"),
    ExcludePattern(".*chromosome-mappings/README.md")
  )

  object test extends Tests with ScalaTest {
    override def moduleDeps = Seq(commons)
  }
}

object pipelines extends CommonModule {
  override def ivyDeps = Agg(
    ivy"com.fulcrumgenomics::dagr-core::$dagrCoreVersion",
    ivy"com.fulcrumgenomics::dagr-tasks::$dagrCoreVersion"
  )

  override def moduleDeps = Seq(commons)

  def deployLocal = T { super.deployLocal(assembly(), "cvbio-pipelines.jar")  }

  object test extends Tests with ScalaTest {
    override def moduleDeps = Seq(pipelines, commons.test)
  }
}

object tools extends CommonModule {

  override def moduleDeps = Seq(commons)

  def deployLocal = T { super.deployLocal(assembly(), "cvbio.jar")  }

  object test extends Tests with ScalaTest {
    override def moduleDeps = Seq(tools, commons.test)
  }
}
