package io.cvbio.tools.igv

import java.nio.file.Path

import io.cvbio.testing.UnitSpec
import com.fulcrumgenomics.commons.io.PathUtil
import htsjdk.samtools.util.Interval

class IgvCommandTest extends UnitSpec {

  "IgvCommand" should "use the class name as the simple name" in {
    Echo.toString   shouldBe "echo"
    Echo.simpleName shouldBe "echo"
    Exit.toString   shouldBe "exit"
    Exit.simpleName shouldBe "exit"
    New.toString    shouldBe "new"
    New.simpleName  shouldBe "new"
  }

  it should "run through all commands and have them formatted correctly" in {
    Collapse().toString shouldBe "collapse"
    Collapse(Some("track1")).toString shouldBe "collapse track1"

    Expand().toString shouldBe "expand"
    Expand(Some("track1")).toString shouldBe "expand track1"

    Genome("mm10").toString shouldBe "genome mm10"
    Genome(PathUtil.pathTo("temp")).toString should fullyMatch regex """genome\s.*temp"""

    Goto("all").toString shouldBe "goto all"
    Goto(Seq("all")).toString shouldBe "goto all"
    Goto(Seq("flt3", "npm1")).toString shouldBe "goto flt3 npm1"
    Goto(new Interval("chr1", 2, 3)).toString shouldBe "goto chr1:2-3"

    val loadPaths: Seq[Path] = Seq("temp1", "temp2").map(PathUtil.pathTo(_))

    Load("filepath").toString shouldBe "load filepath"
    Load(PathUtil.pathTo("temp")).toString should fullyMatch regex """load\s.*temp"""
    Load(loadPaths).toString should fullyMatch regex """load\s.*temp1,.*temp2"""

    Region(new Interval("chr1", 2, 3)).toString shouldBe "region chr1 2 3"

    MaxPanelHeight(200).toString shouldBe "maxPanelHeight 200"

    SetLogScale(true).toString shouldBe "setLogScale true"

    SetSleepInterval(2000).toString shouldBe "setSleepInterval 2000.0"

    SnapshotDirectory(PathUtil.pathTo("dir")).toString should fullyMatch regex """snapshotDirectory.*dir"""

    Snapshot(PathUtil.pathTo("temp1")).toString should fullyMatch regex """snapshot.*temp1"""

    Sort("option", "locus").toString shouldBe "sort option locus"

    Squish().toString shouldBe "squish"
    Squish(Some("track1")).toString shouldBe "squish track1"

    ViewAsPairs().toString shouldBe "viewAsPairs"
    ViewAsPairs(Some("track1")).toString shouldBe "viewAsPairs track1"

    Preference("key", "value").toString shouldBe "preference key value"
  }
}
