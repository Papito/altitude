package software.altitude.core.service.migration

import org.slf4j.LoggerFactory
import software.altitude.core.dao.sqlite
import software.altitude.core.transactions.TransactionId
import software.altitude.core.{Altitude, Context}

class SqliteMigrationService(app: Altitude) extends JdbcMigrationService(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  val MIGRATIONS_DIR = "sqlite/"
  override val DAO = new sqlite.MigrationDao(app)

  log.info("SQLITE migration service initialized")

  override def existingVersion(implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    // cannot open a readonly connection for a non-existing DB
    txManager.withTransaction[Int] {
      DAO.currentVersion
    }
  }

}