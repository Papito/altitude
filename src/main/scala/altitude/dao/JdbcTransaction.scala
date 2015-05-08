package altitude.dao

import java.sql.Connection
import javax.sql.DataSource

import altitude.Util.log
import altitude.{Const => C}

class JdbcTransaction extends Transaction {
  Transaction.CREATED += 1
  private val dsName = "" //FIXME Play.current.configuration.getString("datasource").getOrElse("")
  require(!dsName.isEmpty)
  private val ds: DataSource = null //FIXME
  val conn: Connection = ds.getConnection

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

  def setReadOnly(flag: Boolean) = if (!isNested) conn.setReadOnly(flag)
  def setAutoCommit(flag: Boolean) =if (!isNested) conn.setAutoCommit(flag)
}