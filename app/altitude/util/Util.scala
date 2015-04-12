package altitude

import org.joda.time.{DateTimeZone, DateTime}

package object util {
  def utcNow: DateTime = new DateTime().withZoneRetainFields(DateTimeZone.forID("UTC"))
}
