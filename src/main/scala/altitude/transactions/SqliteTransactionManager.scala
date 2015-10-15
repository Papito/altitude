package altitude.transactions

import java.sql.{DriverManager, Connection}

import altitude.Altitude
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteConfig

class SqliteTransactionManager(app: Altitude, txContainer: scala.collection.mutable.Map[Int, JdbcTransaction])
  extends JdbcTransactionManager(app, txContainer) {
  private final val log = LoggerFactory.getLogger(getClass)

  override def connection(readOnly: Boolean = false): Connection = {
    val url = app.config.getString("db.sqlite.url")

    val conn = readOnly match {
      case true => {
        val config: SQLiteConfig = new SQLiteConfig()
        config.setReadOnly(true)
        DriverManager.getConnection(url, config.toProperties)
      }
      case false => {
        val conn = DriverManager.getConnection(url)
        conn.setAutoCommit(false)
        conn
      }
    }

    log.info(s"Opening connection $conn. Read-only: $readOnly")
    conn
  }

  override protected def closeTransaction(tx: JdbcTransaction) = {
    closeConnection(tx.getConnection)
  }
}
