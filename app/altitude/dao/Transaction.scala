package altitude.dao

import java.sql.Connection
import javax.sql.DataSource

import altitude.util.log
import play.api.db.DB
import play.api.Play.current

class Transaction {
  private val ds: DataSource = DB.getDataSource("postgres")
  private val conn: Connection = ds.getConnection
  val id = scala.util.Random.nextInt(java.lang.Integer.MAX_VALUE)
  var level: Int  = 0

  log.debug(s"New transaction $id")
  def getConnection: Connection = conn

  def isNested: Boolean = level > 0

  def close() = {
    if (level == 0) {
      log.debug(s"Closing connection for transaction $id")
      conn.close()
    }
  }

  def commit() {
    if (level == 0) {
      log.debug(s"Committing transaction $id")
      conn.commit()
    }
  }

  def rollback() {
    if (level == 0) {
      log.debug(s"ROLLBACK for transaction $id")
      conn.rollback()
    }
  }

  def setReadOnly(flag: Boolean) = conn.setReadOnly(flag)
  def setAutoCommit(flag: Boolean) = conn.setAutoCommit(flag)

}