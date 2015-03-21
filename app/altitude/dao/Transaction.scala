package altitude.dao

import java.sql.Connection
import javax.sql.DataSource

class Transaction(val ds: DataSource) {
  val conn: Connection = ds.getConnection
  def commit() = conn.commit()
  def rollback() = conn.rollback()
}