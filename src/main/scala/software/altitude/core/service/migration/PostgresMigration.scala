package software.altitude.core.service.migration

import software.altitude.core.AltitudeAppContext
import software.altitude.core.dao.MigrationDao

trait PostgresMigration { this: CoreMigrationService =>
  protected val app: AltitudeAppContext
  protected val DAO: MigrationDao
}
