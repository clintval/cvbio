package io.cvbio.testing

import com.fulcrumgenomics.bam.Template
import com.fulcrumgenomics.bam.api.SamRecord
import com.fulcrumgenomics.testing.SamBuilder

import scala.collection.mutable.ListBuffer

/** Builder for making [[com.fulcrumgenomics.bam.Template]] objects. */
class TemplateBuilder(name: String) {

  /** The underlying [[SamBuilder]]. */
  val samBuilder: SamBuilder = new SamBuilder()

  var r1: Option[SamRecord] = None
  var r2: Option[SamRecord] = None
  val r1Secondaries: ListBuffer[SamRecord]   =  ListBuffer[SamRecord]()
  val r2Secondaries: ListBuffer[SamRecord]   =  ListBuffer[SamRecord]()
  val r1Supplementals: ListBuffer[SamRecord] =  ListBuffer[SamRecord]()
  val r2Supplementals: ListBuffer[SamRecord] =  ListBuffer[SamRecord]()

  /** Build the template. */
  def template: Template = {
    new Template(
      r1              = r1,
      r2              = r2,
      r1Secondaries   = r1Secondaries,
      r2Secondaries   = r2Secondaries,
      r1Supplementals = r1Supplementals,
      r2Supplementals = r2Supplementals)
  }

  /** Make a primary read pair. */
  private def makePrimaryPair(
    r1Attrs: Map[String, Any] = Map.empty,
    r2Attrs: Map[String, Any] = Map.empty
  ): (SamRecord, SamRecord) = {
    val reads: Seq[SamRecord] = this.samBuilder.addPair(name)
    val (read1, read2): (SamRecord, SamRecord) = (reads.head, reads.last)
    r1Attrs.foreach(keyValue => read1(keyValue._1) = keyValue._2)
    r2Attrs.foreach(keyValue => read2(keyValue._1) = keyValue._2)
    (read1, read2)
  }

  /** Add a primary read pair. */
  def addPrimaryPair(
    r1Attrs: Map[String, Any] = Map.empty,
    r2Attrs: Map[String, Any] = Map.empty
  ): (SamRecord, SamRecord) = {
    val (read1, read2): (SamRecord, SamRecord) = makePrimaryPair(r1Attrs, r2Attrs)
    r1 = Some(read1)
    r2 = Some(read2)
    (read1, read2)
  }

  /** Add a secondary read pair. */
  def addSecondaryPair(
    r1Attrs: Map[String, Any] = Map.empty,
    r2Attrs: Map[String, Any] = Map.empty
  ): (SamRecord, SamRecord) = {
    val (read1, read2): (SamRecord, SamRecord) = makePrimaryPair(r1Attrs, r2Attrs)
    read1.secondary = true
    read2.secondary = true
    r1Secondaries.append(read1)
    r2Secondaries.append(read2)
    (read1, read2)
  }

  /** Add a supplementary read pair. */
  def addSupplementaryPair(
    r1Attrs: Map[String, Any] = Map.empty,
    r2Attrs: Map[String, Any] = Map.empty
  ): (SamRecord, SamRecord) = {
    val (read1, read2): (SamRecord, SamRecord) = makePrimaryPair(r1Attrs, r2Attrs)
    read1.supplementary = true
    read2.supplementary = true
    r1Supplementals.append(read1)
    r2Supplementals.append(read2)
    (read1, read2)
  }
}
