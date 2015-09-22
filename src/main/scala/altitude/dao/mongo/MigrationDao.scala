package altitude.dao.mongo

import altitude.Altitude
import altitude.transactions.TransactionId

class MigrationDao(app: Altitude) extends altitude.dao.MigrationDao(app) {
  def currentVersion(implicit txId: TransactionId = new TransactionId): Int = 1
  def versionUp(implicit txId: TransactionId = new TransactionId): Unit = Unit
}
