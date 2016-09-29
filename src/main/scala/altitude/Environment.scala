package altitude

import java.io.File

object Environment extends Enumeration {
  val TEST, PROD, DEV = Value
  var ENV = PROD

  def root: String = ENV match  {
    case PROD => {
      val url = Environment.getClass.getProtectionDomain.getCodeSource.getLocation
      val path = new File(url.toURI).getParentFile.getPath
      File.separator + path + File.separator
    }
    case _ => ""
  }
}
