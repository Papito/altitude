package altitude.service

import altitude.Altitude
import altitude.dao.{sqlite, MigrationDao}
import org.slf4j.LoggerFactory

class SqliteMigrationService(app: Altitude) extends JdbcMigrationService(app) {
  private val log =  LoggerFactory.getLogger(getClass)
  val EVOLUTIONS_DIR = "sqlite/"
  override val DAO = new sqlite.MigrationDao(app)

  log.info("SQLITE migration service initialized")
}