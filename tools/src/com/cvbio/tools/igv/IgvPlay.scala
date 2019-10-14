package com.cvbio.tools.igv

import scala.collection.mutable.ListBuffer

/** A sequence of [[IgvCommand]]s.  */
class IgvPlay private(private val commands: ListBuffer[IgvCommand]) extends Iterable[IgvCommand] {

  /** Iterator over the [[IgvCommand]]s. */
  def iterator: Iterator[IgvCommand] = commands.iterator

  /** The number of commands within this [[IgvPlay]]. */
  def length: Int = this.commands.length

  /** Append an item to this [[IgvPlay]]. */
  def add(item: IgvCommand): this.type = {
    commands += item
    this
  }

  /** Append an item to this [[IgvPlay]]. */
  def add(item: Seq[IgvCommand]): this.type = {
    commands ++= item
    this
  }

  /** Append an item to this [[IgvPlay]]. */
  def +=(item: IgvCommand): this.type = this.add(item)

  /** Append all items to this [[IgvPlay]]. */
  def ++=(items: TraversableOnce[IgvCommand]): this.type = {
    commands ++= items
    this
  }
}

/** Companion object for [[IgvPlay]]. */
object IgvPlay {

  /** Build an empty [[IgvPlay]]. */
  def apply(): IgvPlay = new IgvPlay(new ListBuffer[IgvCommand]())
}
