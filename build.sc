import ammonite.ops._
import coursier.maven.MavenRepository
import mill._
import mill.scalalib._

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

object tools extends CommonModule {
  override def ivyDeps = Agg(
    ivy"org.apache.httpcomponents:httpclient:4.5.8",
    ivy"com.fulcrumgenomics::commons::$fgbioCommonsVersion",
    ivy"com.fulcrumgenomics::fgbio::$fgbioVersion".excludeOrg(excludeOrg: _*),
  )

  def deployLocal = T { super.deployLocal(assembly(), "cvbio.jar")  }
}
