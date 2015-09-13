package altitude

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}

package object Util {

  def utcNow: DateTime = new DateTime().withZoneRetainFields(DateTimeZone.forID("UTC"))

  def isoDateTime (dt: Option[DateTime]): String = {
    if (dt.isDefined) ISODateTimeFormat.dateTime().print(dt.get) else ""
  }

  def randomStr(size: Int = 10) = scala.util.Random.alphanumeric.take(size).mkString

  private val DELIM_REGEX = List(" ",",").mkString("|").r
  def parseKeywords(s: String): Set[String] =
    DELIM_REGEX.split(s).map(_.trim).filter(_ != "").toSet
}
