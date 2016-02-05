package altitude

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}

package object Util {

  def utcNow: DateTime = new DateTime().withZoneRetainFields(DateTimeZone.forID("UTC"))

  def isoDateTime (dt: Option[DateTime]): String = {
    if (dt.isDefined) ISODateTimeFormat.dateTime().print(dt.get) else ""
  }

  def randomStr(size: Int = 10) = scala.util.Random.alphanumeric.take(size).mkString

  // TODO: move into the keyword code when it's done
  private val DELIM_REGEX = List(" ",",").mkString("|").r
  def parseKeywords(s: String): Set[String] =
    DELIM_REGEX.split(s).toSeq.map(_.trim.toLowerCase).filter(_.nonEmpty).toSet
}
