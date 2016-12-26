package altitude.exceptions

/**
 * All-purpose event to get out of loops with user interrupts or conditionals
 */
case class AllDone() extends Exception
