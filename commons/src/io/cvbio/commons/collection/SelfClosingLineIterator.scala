package io.cvbio.commons.collection

import java.io.BufferedReader

import com.fulcrumgenomics.commons.collection.SelfClosingIterator

/** Read lines from a [[java.io.BufferedReader]] and close the underlying reader once exhausted. */
class SelfClosingLineIterator(reader: BufferedReader)
  extends SelfClosingIterator(
    iter   = Iterator.continually(reader.readLine()).takeWhile(_ != null),
    closer = () => reader.close()
  )
