package io.cvbio.bam

import io.cvbio.bam.Bams.ReadOrdinal.{All, Read1, Read2}
import io.cvbio.commons.CommonsDef._
import io.cvbio.commons.effectful.Io
import com.fulcrumgenomics.FgBioDef.{DirPath, FgBioEnum}
import com.fulcrumgenomics.bam.Bams.sorter
import com.fulcrumgenomics.bam.Template
import com.fulcrumgenomics.bam.api.SamOrder.Queryname
import com.fulcrumgenomics.bam.api.{SamOrder, SamRecord, SamSource}
import com.fulcrumgenomics.commons.collection.{BetterBufferedIterator, SelfClosingIterator}
import com.fulcrumgenomics.commons.util.LazyLogging
import com.fulcrumgenomics.util.ProgressLogger
import enumeratum.EnumEntry
import htsjdk.samtools.util.CloserUtil
import htsjdk.samtools.{SAMFileHeader => SamFileHeader}

/** Common methods for working with SAM/BAM files. */
object Bams extends LazyLogging {

  /** The default max records to keep in memory when sorting. */
  val DefaultMaxRecordsInMemory: Int = 1e6.toInt

  /** The default temporary directory for disk-backed sorting. */
  lazy val DefaultSortingTempDirectory: DirPath = Io.makeTempDir(name = "BamSort")

  /** Implicit class that makes working with a [[Template]] easier. */
  implicit class TemplateUtil(private val template: Template) {

    /** Return the SAM tags across a specific read ordinal. */
    def tagValues[T](ordinal: ReadOrdinal, tag: SamTag): Seq[Option[T]] = {
      ordinal match {
        case All   => template.allReads.toSeq.map(_.get[T](tag))
        case Read1 => (template.r1 ++: template.r1Secondaries ++: template.r1Supplementals).map(_.get[T](tag))
        case Read2 => (template.r2 ++: template.r2Secondaries ++: template.r2Supplementals).map(_.get[T](tag))
      }
    }
  }

  /** Trait that all enumeration values of type [[ReadOrdinal]] should extend. */
  sealed trait ReadOrdinal extends EnumEntry with Product with Serializable

  /** Contains enumerations of read ordinals. */
  object ReadOrdinal extends FgBioEnum[ReadOrdinal] {

    /** Return all read ordinals. */
    def values: scala.collection.immutable.IndexedSeq[ReadOrdinal] = findValues

    /** All read ordinals. */
    case object All extends ReadOrdinal

    /** The read ordinal for read one. */
    case object Read1 extends ReadOrdinal

    /** The read ordinal for read two. */
    case object Read2 extends ReadOrdinal
  }

  /** Collectively iterate through [[SamSource]] iterators and emit templates of the same query.
    *
    * Reads will be grouped and sorted into queryname order if they are not already. All [[SamSource]] must contain
    * the same templates by query name.
    *
    * @param sources the SAM sources to iterate over
    * @return a self-closing iterator over all templates in all SAM sources sorted by name
    */
  def templatesIterator(sources: SamSource*): SelfClosingIterator[Seq[Template]] = {
    val iterator: Iterator[Seq[Template]] = new Iterator[Seq[Template]] {

      /** The underlying template iterators. */
      private val iterators = sources.map(templateSortedIterator(_))

      /** Test all template iterators to see if they have another template to emit. */
      override def hasNext: Boolean = {
        iterators
          .map(_.hasNext)
          .ensuring(_.distinct.length <= 1, "SAM sources do not have the same number of templates!")
          .contains(true)
      }

      /** Advance to the next sequence of templates. */
      override def next(): Seq[Template] = {
        require(hasNext, "next() called on empty iterator")
        val templates = iterators.map(_.next)
        require(
          templates.map(_.name).distinct.length <= 1,
          "Templates with different names found! This can only occur if your SAM sources are queryname sorted using"
            + " different implementations, such as with Picard tools versus Samtools. If you have encountered this"
            + " exception, then please alert the maintainer!"
        )
        templates
      }
    }

    new SelfClosingIterator(iterator, () => sources.foreach(_.safelyClose()))
  }

  /** Returns an iterator over records in such a way that all reads with the same query name are adjacent in the
    * iterator. Although a queryname sort is guaranteed, the sort order may not be consistent with other queryname
    * sorting implementations, especially in other tool kits.
    *
    * @param in a SamReader from which to consume records
    * @param maxInMemory the maximum number of records to keep and sort in memory, if sorting is needed
    * @param tmpDir a temp directory to use for temporary sorting files if sorting is needed
    * @return an Iterator with reads from the same query grouped together
    */
  def querySortedIterator(
    in: SamSource,
    maxInMemory: Int = DefaultMaxRecordsInMemory,
    tmpDir: DirPath  = DefaultSortingTempDirectory
  ): BetterBufferedIterator[SamRecord] = querySortedIterator(in.iterator, in.header, maxInMemory, tmpDir)

  /** Returns an iterator over records in such a way that all reads with the same query name are adjacent in the
    * iterator. Although a queryname sort is guaranteed, the sort order may not be consistent with other queryname
    * sorting implementations, especially in other tool kits.
    *
    * @param iterator an iterator from which to consume records
    * @param header the header associated with the records
    * @param maxInMemory the maximum number of records to keep and sort in memory, if sorting is needed
    * @param tmpDir a temp directory to use for temporary sorting files if sorting is needed
    * @return an Iterator with reads from the same query grouped together
    */
  def querySortedIterator(
    iterator: Iterator[SamRecord],
    header: SamFileHeader,
    maxInMemory: Int,
    tmpDir: DirPath
  ): SelfClosingIterator[SamRecord] = {
    (SamOrder(header), iterator) match {
      case (Some(Queryname), _iterator: SelfClosingIterator[SamRecord]) => _iterator
      case (Some(Queryname), _) => new SelfClosingIterator(iterator.bufferBetter, () => CloserUtil.close(iterator))
      case (_, _) =>
        logger.info(parts = "Sorting into queryname order.")
        val progress = ProgressLogger(this.logger, "Records", "sorted")
        val sort     = sorter(Queryname, header, maxInMemory, tmpDir)
        iterator.tapEach(progress.record).foreach(sort.write)
        new SelfClosingIterator(sort.iterator, () => sort.close())
    }
  }

  /** Return an iterator over records sorted and grouped into [[Template]] objects. Although a queryname sort is
    * guaranteed, the sort order may not be consistent with other queryname sorting implementations, especially in other
    * tool kits. See [[com.fulcrumgenomics.bam.Bams.templateIterator]] for a [[Template]] iterator which emits templates
    * in a non-guaranteed sort order.
    *
    * @see [[com.fulcrumgenomics.bam.Bams.templateIterator]]
    *
    * @param in a [[SamSource]] from which to consume records
    * @param maxInMemory the maximum number of records to keep and sort in memory, if sorting is needed
    * @param tmpDir an optional temp directory to use for temporary sorting files if needed
    * @return an [[Iterator]] of query sorted [[Template]] objects
    */
  def templateSortedIterator(
    in: SamSource,
    maxInMemory: Int = DefaultMaxRecordsInMemory,
    tmpDir: DirPath  = DefaultSortingTempDirectory
  ): SelfClosingIterator[Template] = templateSortedIterator(in.iterator, in.header, maxInMemory, tmpDir)

  /** Return an iterator over records sorted and grouped into [[Template]] objects. Although a queryname sort is
    * guaranteed, the sort order may not be consistent with other queryname sorting implementations, especially in other
    * tool kits. See [[com.fulcrumgenomics.bam.Bams.templateIterator]] for a [[Template]] iterator which emits templates
    * in a non-guaranteed sort order.
    *
    * @see [[com.fulcrumgenomics.bam.Bams.templateIterator]]
    *
    * @param iterator an iterator from which to consume records
    * @param header the header associated with the records
    * @param maxInMemory the maximum number of records to keep and sort in memory, if sorting is needed
    * @param tmpDir a temp directory to use for temporary sorting files if sorting is needed
    * @return an Iterator of queryname sorted Template objects
    */
  def templateSortedIterator(
    iterator: Iterator[SamRecord],
    header: SamFileHeader,
    maxInMemory: Int,
    tmpDir: DirPath
  ): SelfClosingIterator[Template] = {
    val queryIterator = querySortedIterator(iterator, header, maxInMemory, tmpDir)

    val _iterator = new Iterator[Template] {
      override def hasNext: Boolean = queryIterator.hasNext
      override def next: Template   = {
        require(hasNext, "next() called on empty iterator")
        Template(queryIterator)
      }
    }

    new SelfClosingIterator(_iterator, () => queryIterator.close())
  }
}
