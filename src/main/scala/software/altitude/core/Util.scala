package software.altitude.core
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory

import java.io.PrintWriter
import java.io.StringWriter

object Util {
  private final val log = LoggerFactory.getLogger(getClass)

  def logStacktrace(e: Exception): String = {
    e.printStackTrace()
    val sw: StringWriter = new StringWriter()
    val pw: PrintWriter = new PrintWriter(sw)
    e.printStackTrace(pw)
    val strStacktrace = sw.toString
    log.error(s"${e.getClass.getName} exception: $strStacktrace")
    strStacktrace
  }

  def isoDateTime (dt: Option[DateTime]): String = {
    if (dt.isDefined) ISODateTimeFormat.dateTime().print(dt.get) else ""
  }

  def randomStr(size: Int = 10): String = scala.util.Random.alphanumeric.take(size).mkString

  def randomLongInt(): Long = scala.util.Random.nextLong(1000000000L)


  def hashPassword(password: String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt())
  }

  def checkPassword(password: String, hashedPassword: String): Boolean = {
    BCrypt.checkpw(password, hashedPassword)
  }}
