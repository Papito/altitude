package altitude.transactions

import java.sql.Connection

import altitude.{Const => C}
import org.slf4j.LoggerFactory

class JdbcTransaction(val conn: Connection) extends Transaction {
  val log =  LoggerFactory.getLogger(getClass)

  Transaction.CREATED += 1

  log.debug(s"New JDBC transaction $id", C.tag.DB)
  def getConnection: Connection = conn

  override def close() = {
    if (!isNested) {
      log.debug(s"Closing connection for transaction $id", C.tag.DB)
      Transaction.CLOSED += 1
      conn.close()
    }
  }

  override def commit() {
    if (!isNested) {
      log.debug(s"Committing transaction $id", C.tag.DB)
      Transaction.COMMITTED += 1
      conn.commit()
    }
  }

  override def rollback() {
    if (!isNested && !conn.isReadOnly) {
      log.warn(s"ROLLBACK for transaction $id", C.tag.DB)
      Transaction.ROLLED_BACK += 1
      conn.rollback()
    }
  }

  override def setReadOnly(flag: Boolean): Unit = if (!isNested) conn.setReadOnly(flag)
  override def setAutoCommit(flag: Boolean): Unit = if (!isNested) conn.setAutoCommit(flag)
}