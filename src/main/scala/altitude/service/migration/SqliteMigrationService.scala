package altitude.service.migration

import altitude.dao.sqlite
import altitude.{Altitude, Context}
import org.slf4j.LoggerFactory

class SqliteMigrationService(app: Altitude) extends JdbcMigrationService(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  val MIGRATIONS_DIR = "sqlite/"
  override val DAO = new sqlite.MigrationDao(app)

  log.info("SQLITE migration service initialized")

  override def existingVersion(implicit ctx: Context = new Context): Int = {
    // cannot open a readonly connection for a non-existing DB
    txManager.withTransaction[Int] {
      DAO.currentVersion
    }
  }

}