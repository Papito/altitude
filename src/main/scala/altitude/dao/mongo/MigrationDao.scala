package altitude.dao.mongo

import altitude.Altitude

class MigrationDao(app: Altitude) extends altitude.dao.MigrationDao(app) {
  def currentVersion: Int = 1
  def versionUp(): Unit = Unit
}
