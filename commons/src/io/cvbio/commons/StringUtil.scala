package io.cvbio.commons

/** Common String utilities. */
object StringUtil {

  /** Lowercase the first character of a string. */
  def uncapitilize(s: String): String = {
    if (s.isEmpty) s
    else if (s.lengthCompare(1) == 0) s.toLowerCase
    else s.charAt(0).toLower + s.substring(1)
  }
}
