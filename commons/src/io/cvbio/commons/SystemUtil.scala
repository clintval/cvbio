package io.cvbio.commons

/** Helper methods for working with the system. */
object SystemUtil {

  /** The system cores reported by this runtime. */
  val availableCores: Int = Runtime.getRuntime.availableProcessors
}
