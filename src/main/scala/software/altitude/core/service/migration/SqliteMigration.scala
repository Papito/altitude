package software.altitude.core.service.migration

import org.slf4j.LoggerFactory
import software.altitude.core.dao.MigrationDao
import software.altitude.core.transactions.{AbstractTransactionManager, TransactionId}
import software.altitude.core.{AltitudeCoreApp, Context}

trait SqliteMigration { this: CoreMigrationService =>
  private final val log = LoggerFactory.getLogger(getClass)

  protected val MIGRATIONS_DIR = "sqlite/"

  protected val app: AltitudeCoreApp
  protected val DAO: MigrationDao
  protected val txManager: AbstractTransactionManager

  override def existingVersion(implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    // cannot open a readonly connection for a non-existing DB
    txManager.withTransaction[Int] {
      DAO.currentVersion
    }
  }

}
