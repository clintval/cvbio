import ammonite.ops._
import coursier.maven.MavenRepository
import mill._
import mill.modules.Assembly.Rule.ExcludePattern
import mill.scalalib._

private val dagrCoreVersion     = "0.6.0-f784de2-SNAPSHOT"
private val fgbioCommonsVersion = "0.8.0-c93e0f3-SNAPSHOT"
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

object commons extends CommonModule {

  override def ivyDeps = Agg(
    ivy"org.apache.httpcomponents:httpclient:4.5.8",
    ivy"com.fulcrumgenomics::commons::$fgbioCommonsVersion",
    ivy"com.fulcrumgenomics::fgbio::$fgbioVersion".excludeOrg(excludeOrg: _*),
    ivy"org.reflections:reflections:0.9.11"
  )

  override def assemblyRules = Seq(
    ExcludePattern(".*\\.git.*"),
    ExcludePattern(".*chromosome-mappings/README.md")
  )
}

object pipelines extends CommonModule {
  override def ivyDeps = Agg(
    ivy"com.fulcrumgenomics::dagr-core::$dagrCoreVersion",
    ivy"com.fulcrumgenomics::dagr-tasks::$dagrCoreVersion"
  )

  override def moduleDeps = Seq(commons)

  def deployLocal = T { super.deployLocal(assembly(), "cvbio-pipelines.jar")  }
}

object tools extends CommonModule {

  override def moduleDeps = Seq(commons)

  def deployLocal = T { super.deployLocal(assembly(), "cvbio.jar")  }
}
