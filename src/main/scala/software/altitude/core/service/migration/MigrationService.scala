package software.altitude.core.service.migration

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude

abstract class MigrationService(val app: Altitude) extends CoreMigrationService {
  protected final val log: Logger = LoggerFactory.getLogger(getClass)

  def migrateVersion(version: Int): Unit = {
      version match {
        case 1 => v1()
      }
  }

  private def v1(): Unit = {
  }
}
