package com.cvbio.testing

import org.scalatest.{FlatSpec, Matchers, OptionValues}

/** Base class for unit testing. */
trait UnitSpec extends FlatSpec with Matchers with OptionValues
