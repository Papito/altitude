package software.altitude.core.dao

import software.altitude.core.Context
import software.altitude.core.transactions.TransactionId

trait MigrationDao {
  def currentVersion(implicit ctx: Context, txId: TransactionId): Int
  def versionUp()(implicit ctx: Context, txId: TransactionId): Unit
  def executeCommand(command: String)(implicit ctx: Context, txId: TransactionId): Unit
}
