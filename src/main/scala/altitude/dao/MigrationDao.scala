package altitude.dao

import altitude.dao.jdbc.VoidJdbcDao
import altitude.transactions.TransactionId
import altitude.{Altitude, Context}

abstract class MigrationDao(app: Altitude) extends VoidJdbcDao(app) {
  def currentVersion(implicit ctx: Context, txId: TransactionId): Int
  def versionUp()(implicit ctx: Context, txId: TransactionId): Unit
  def executeCommand(command: String)(implicit ctx: Context, txId: TransactionId): Unit
}
