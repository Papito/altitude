package altitude.dao

import java.sql.Connection
import javax.sql.DataSource

import play.api.db.DB
import play.api.Play.current

class Transaction {
  private val ds: DataSource = DB.getDataSource("postgres")
  val conn: Connection = ds.getConnection
  def commit() = conn.commit()
  def rollback() = conn.rollback()
}