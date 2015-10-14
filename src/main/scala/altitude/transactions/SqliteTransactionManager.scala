package altitude.transactions

import java.sql.{DriverManager, Connection}

import altitude.Altitude
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteConfig

class SqliteTransactionManager(app: Altitude) extends JdbcTransactionManager(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  override def connection: Connection = {
    val url = app.config.getString("db.sqlite.url")
    DriverManager.getConnection(url)
  }

  def readOnlyConnection: Connection = {
    val url = app.config.getString("db.sqlite.url")
    val config: SQLiteConfig = new SQLiteConfig();
    config.setReadOnly(true)
    DriverManager.getConnection(url, config.toProperties)
  }
  
  override protected def closeTransaction(tx: JdbcTransaction) = {
    closeConnection(tx.getConnection)
  }

  override protected def setReadOnly(tx: JdbcTransaction, value: Boolean): Unit = {
  }

}
