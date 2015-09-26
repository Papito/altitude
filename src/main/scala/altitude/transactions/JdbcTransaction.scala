package altitude.transactions

import java.sql.Connection

import altitude.{Const => C}
import org.slf4j.LoggerFactory

class JdbcTransaction(val conn: Connection) extends Transaction {
  private final val log = LoggerFactory.getLogger(getClass)

  log.debug(s"New JDBC transaction $id", C.LogTag.DB)
  def getConnection: Connection = conn

  override def close() = {
    if (!isNested) {
      log.debug(s"Closing connection for transaction $id", C.LogTag.DB)
      conn.close()
    }
  }

  override def commit() {
    if (!isNested) {
      log.debug(s"Committing transaction $id", C.LogTag.DB)
      conn.commit()
    }
  }

  override def rollback() {
    if (!isNested && !conn.isReadOnly) {
      log.warn(s"ROLLBACK for transaction $id", C.LogTag.DB)
      conn.rollback()
    }
  }

  override def setReadOnly(flag: Boolean): Unit = if (!isNested) conn.setReadOnly(flag)
  override def setAutoCommit(flag: Boolean): Unit = if (!isNested) conn.setAutoCommit(flag)
}