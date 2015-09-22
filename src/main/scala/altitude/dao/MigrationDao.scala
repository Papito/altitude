package altitude.dao

import altitude.Altitude
import altitude.dao.jdbc.VoidJdbcDao
import altitude.transactions.TransactionId

abstract class MigrationDao(app: Altitude) extends VoidJdbcDao(app) {
  def currentVersion(implicit txId: TransactionId): Int
  def versionUp(implicit txId: TransactionId): Unit
}
