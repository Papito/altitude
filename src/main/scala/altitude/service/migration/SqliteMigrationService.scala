package altitude.service.migration

import altitude.Altitude
import altitude.dao.sqlite
import altitude.transactions.TransactionId
import org.slf4j.LoggerFactory

class SqliteMigrationService(app: Altitude) extends JdbcMigrationService(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  val MIGRATIONS_DIR = "sqlite/"
  override val DAO = new sqlite.MigrationDao(app)

  log.info("SQLITE migration service initialized")

  override def existingVersion(implicit txId: TransactionId = new TransactionId): Int = {
    // cannot open a readonly connection for a non-existing DB
    txManager.withTransaction[Int] {
      DAO.currentVersion
    }
  }

}