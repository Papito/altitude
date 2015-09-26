package altitude.service.migration

import altitude.Altitude
import altitude.dao.postgres
import org.slf4j.LoggerFactory

class PostgresMigrationService(app: Altitude) extends JdbcMigrationService(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  val MIGRATIONS_DIR = "postgres/"
  override val DAO = new postgres.MigrationDao(app)

  log.info("POSTGRES migration service initialized")
}
