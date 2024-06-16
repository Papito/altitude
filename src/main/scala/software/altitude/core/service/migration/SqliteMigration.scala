package software.altitude.core.service.migration

import software.altitude.core.AltitudeAppContext
import software.altitude.core.Context
import software.altitude.core.transactions.TransactionId

trait SqliteMigration { this: CoreMigrationService =>
  protected val app: AltitudeAppContext

  override def existingVersion(implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    // cannot open a readonly connection for a non-existing DB
    txManager.withTransaction[Int] {
      DAO.currentVersion
    }
  }

}
