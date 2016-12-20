package altitude.dao

import altitude.dao.jdbc.VoidJdbcDao
import altitude.{Altitude, Context}

abstract class MigrationDao(app: Altitude) extends VoidJdbcDao(app) {
  def currentVersion(implicit ctx: Context): Int
  def versionUp()(implicit ctx: Context): Unit
  def executeCommand(command: String)(implicit ctx: Context): Unit
}
