package altitude.dao

import java.sql.Connection
import javax.sql.DataSource

import altitude.util.log
import play.api.Play
import play.api.db.DB
import play.api.Play.current

object JdbcTransaction {
  lazy val dsName = Play.current.configuration.getString("datasource").getOrElse("")
  require(!dsName.isEmpty)
  lazy val ds: DataSource = DB.getDataSource(dsName)
}

class JdbcTransaction extends Transaction {
  private val conn: Connection = JdbcTransaction.ds.getConnection

  log.debug(s"New transaction $id")
  def getConnection: Connection = conn

  override def close() = {
    if (level == 0) {
      log.debug(s"Closing connection for transaction $id")
      conn.close()
    }
  }

  override def commit() {
    if (level == 0) {
      log.debug(s"Committing transaction $id")
      conn.commit()
    }
  }

  override def rollback() {
    if (level == 0) {
      log.debug(s"ROLLBACK for transaction $id")
      conn.rollback()
    }
  }

  override def setReadOnly(flag: Boolean) = conn.setReadOnly(flag)
  override def setAutoCommit(flag: Boolean) = conn.setAutoCommit(flag)
}