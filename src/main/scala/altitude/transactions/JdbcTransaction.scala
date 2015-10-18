package altitude.transactions

import java.sql.Connection

import altitude.{Const => C}
import org.slf4j.LoggerFactory

class JdbcTransaction(private val conn: Connection) extends Transaction {
  private final val log = LoggerFactory.getLogger(getClass)

  log.debug(s"New JDBC transaction $id", C.LogTag.DB)
  def getConnection: Connection = conn

  override def close() = {
    if (!isNested) {
      // FIXME: try/catch/log to avoid hanging lock on error
      log.debug(s"Closing connection for transaction $id", C.LogTag.DB)
      conn.close()
    }
  }

  override def commit() {
    if (!isNested) {
      // FIXME: try/catch/log to avoid hanging lock on error
      log.debug(s"Committing transaction $id", C.LogTag.DB)
      conn.commit()
    }
  }

  override def rollback() {
    if (!isNested && !conn.isReadOnly) {
      // FIXME: try/catch/log to avoid hanging lock on error
      log.warn(s"ROLLBACK for transaction $id", C.LogTag.DB)
      conn.rollback()
    }
  }
}