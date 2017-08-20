package software.altitude.core.transactions

import java.sql.{Connection, SQLException}

import org.slf4j.LoggerFactory
import software.altitude.core.{Const => C}

/**
 * JDBC transaction object. It is important to note that most/all methods here should NOT THROW.
 * We assume the methods on the connection are called within catch/finally blocks, and therefore
 * we should let those methods finish.
 *
 * @param conn the connection
 * @param isReadOnly is this connection read-only?
 */
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
        case e: SQLException => log.error(s"SQL ERROR closing connection for transaction [$id]: $e")
        case e: Exception => log.error(s"ERROR closing connection for transaction [$id]: $e")
      }
    }
  }

  override def commit() {
    if (!hasParents) {
      try {
        conn.commit()
      }
      catch {
        case e: SQLException => log.error(s"SQL ERROR committing connection for transaction [$id]: $e")
        case e: Exception => log.error(s"ERROR committing connection for transaction [$id]: $e")
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
        case e: SQLException => log.error(s"SQL ERROR rolling back connection for transaction [$id]: $e")
        case e: Exception => log.error(s"ERROR rolling back connection for transaction [$id]: $e")
      }
    }
  }
}