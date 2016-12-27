package altitude.transactions

import java.sql.{SQLException, Connection}

import altitude.{Const => C}
import org.slf4j.LoggerFactory

class JdbcTransaction(private val conn: Connection, val isReadOnly: Boolean) extends Transaction {
  private final val log = LoggerFactory.getLogger(getClass)

  def getConnection: Connection = conn

  override def close() = {
    if (!hasParents) {
      log.debug(s"Closing connection for transaction $id", C.LogTag.DB)
      try {
        conn.close()
      }
      catch {
        case e: SQLException => log.error(s"Error closing connection for transaction [$id]")
      }
    }
  }

  override def commit() {
    if (!hasParents) {
      try {
        conn.commit()
      }
      catch {
        case e: SQLException => log.error(s"Error committing connection for transaction [$id]")
      }
    }
  }

  override def rollback() {
    if (!hasParents && !conn.isReadOnly) {
      log.warn(s"ROLLBACK for transaction $id", C.LogTag.DB)
      try {
        conn.rollback()
      }
      catch {
        case e: SQLException => log.error(s"Error rolling back connection for transaction [$id]")
      }
    }
  }
}