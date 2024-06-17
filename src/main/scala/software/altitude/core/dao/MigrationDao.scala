package software.altitude.core.dao

trait MigrationDao {
  def currentVersion: Int
  def versionUp(): Unit
  def executeCommand(command: String): Unit
}
