package altitude

import java.util.logging.Level

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{JsObject, JsString, Json}

package object Util {

  def utcNow: DateTime = new DateTime().withZoneRetainFields(DateTimeZone.forID("UTC"))

  def isoDateTime (dt: Option[DateTime]): String = {
    if (dt.isDefined) ISODateTimeFormat.dateTime().print(dt.get) else ""
  }
}
