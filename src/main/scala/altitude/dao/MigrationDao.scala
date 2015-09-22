package altitude.dao

import altitude.Altitude
import altitude.dao.jdbc.VoidJdbcDao

abstract class MigrationDao(app: Altitude) extends VoidJdbcDao(app) {
  def currentVersion: Int
  def versionUp(): Unit
}
