package com.cvbio.tools.igv

import scala.collection.mutable.ListBuffer

/** A sequence of [[IgvCommand]]s. */
class IgvPlay(val commands: ListBuffer[IgvCommand] = ListBuffer[IgvCommand]()) extends Iterable[IgvCommand] {

  /** Iterator over the [[IgvCommand]]s. */
  def iterator: Iterator[IgvCommand] = commands.iterator

  /** The number of commands within this [[IgvPlay]]. */
  def length: Int = this.commands.length

  /** Add an item to this [[IgvPlay]]. */
  def +=(item: IgvCommand): this.type = {
    commands += item
    this
  }

  /** Add all items to this [[IgvPlay]]. */
  def ++=(items: TraversableOnce[IgvCommand]): this.type = {
    commands ++= items
    this
  }
}
