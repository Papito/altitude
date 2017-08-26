package software.altitude.core.service.migration

import org.slf4j.LoggerFactory
import software.altitude.core.AltitudeCoreApp
import software.altitude.core.dao.MigrationDao

trait PostgresMigration { this: CoreMigrationService =>
  protected val app: AltitudeCoreApp

  private final val log = LoggerFactory.getLogger(getClass)

  protected val MIGRATIONS_DIR = "postgres/"
  protected val DAO: MigrationDao
}
