package altitude.dao

import java.sql.Connection
import javax.sql.DataSource

import altitude.util.log
import play.api.Play
import play.api.Play.current
import play.api.db.DB

class JdbcTransaction extends Transaction {
  Transaction.CREATED += 1
  private val dsName = Play.current.configuration.getString("datasource").getOrElse("")
  require(!dsName.isEmpty)
  private val ds: DataSource = DB.getDataSource(dsName)
  val conn: Connection = ds.getConnection

  log.debug(s"New JDBC transaction $id")
  def getConnection: Connection = conn

  override def close() = {
    if (level == 0) {
      log.debug(s"Closing connection for transaction $id")
      Transaction.CLOSED += 1
      conn.close()
    }
  }

  override def commit() {
    if (level == 0) {
      log.debug(s"Committing transaction $id")
      Transaction.COMMITTED += 1
      conn.commit()
    }
  }

  override def rollback() {
    if (level == 0 && !conn.isReadOnly) {
      log.debug(s"ROLLBACK for transaction $id")
      Transaction.ROLLED_BACK += 1
      conn.rollback()
    }
  }

  def setReadOnly(flag: Boolean) = conn.setReadOnly(flag)
  def setAutoCommit(flag: Boolean) = conn.setAutoCommit(flag)
}