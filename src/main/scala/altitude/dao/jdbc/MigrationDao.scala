package altitude.dao.jdbc

import altitude.transactions.TransactionId
import altitude.{Const, Altitude}
import org.slf4j.LoggerFactory

class MigrationDao(app: Altitude) extends altitude.dao.MigrationDao(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  def currentVersion(implicit txId: TransactionId = new TransactionId): Int = {
    val sql = s"SELECT * FROM $VERSION_TABLE_NAME"
    val version = try {
      val rec = oneBySqlQuery(sql)
      rec.get("version").asInstanceOf[Int]
    }
    catch {
      // table does not exist
      case ex: java.sql.SQLException => 0
    }
    version
  }

  def executeCommand(command: String)(implicit txId: TransactionId): Unit = {
    val stmt = conn.createStatement()
    stmt.executeUpdate(command)
    stmt.close()
  }

  def versionUp(implicit txId: TransactionId): Unit = Unit

  override def increment(id: String, field: String, count: Int = 1)(implicit txId: TransactionId): Unit = Unit
  override def decrement(id: String, field: String, count: Int = 1)(implicit txId: TransactionId): Unit = Unit
}
