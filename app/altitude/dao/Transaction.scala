package altitude.dao

import java.sql.Connection
import javax.sql.DataSource

import altitude.util.log
import play.api.db.DB
import play.api.Play.current

class Transaction {
  private val ds: DataSource = DB.getDataSource("postgres")
  private val conn: Connection = ds.getConnection
  val id = scala.util.Random.nextInt()

  def getConnection: Connection = conn

  def close() = {
    log.debug("Closing connection for transaction: " + id)
    conn.close()
  }

  def commit() {
    log.debug("Committing transaction: " + id)
    conn.commit()
  }

  def rollback() {
    log.debug("ROLLBACK for transaction: " + id)
    conn.rollback()
  }

  def setReadOnly(flag: Boolean) = conn.setReadOnly(flag)
  def setAutoCommit(flag: Boolean) = conn.setAutoCommit(flag)

}