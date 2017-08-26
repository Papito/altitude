package software.altitude.core.service.migration

import org.slf4j.LoggerFactory
import software.altitude.core.AltitudeCoreApp
import software.altitude.core.dao.MigrationDao

trait PostgresMigration { this: CoreMigrationService =>
  protected val app: AltitudeCoreApp
  protected val DAO: MigrationDao
}
