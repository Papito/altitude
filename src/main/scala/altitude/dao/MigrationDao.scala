package altitude.dao

import altitude.Context
import altitude.transactions.TransactionId

trait MigrationDao {
  def currentVersion(implicit ctx: Context, txId: TransactionId): Int
  def versionUp()(implicit ctx: Context, txId: TransactionId): Unit
  def executeCommand(command: String)(implicit ctx: Context, txId: TransactionId): Unit
}