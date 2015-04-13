package altitude

import org.joda.time.{DateTimeZone, DateTime}

package object Util {
  def utcNow: DateTime = new DateTime().withZoneRetainFields(DateTimeZone.forID("UTC"))
}
