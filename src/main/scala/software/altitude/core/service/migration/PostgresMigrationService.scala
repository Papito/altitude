package software.altitude.core.service.migration

import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.dao.postgres

class PostgresMigrationService(val app: Altitude) extends JdbcMigrationService(app) with ServerMigrations {
  private final val log = LoggerFactory.getLogger(getClass)

  val MIGRATIONS_DIR = "postgres/"
  override val DAO = new postgres.MigrationDao(app)

  log.info("POSTGRES migration service initialized")
}
