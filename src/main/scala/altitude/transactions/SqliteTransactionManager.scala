package altitude.transactions

import java.sql.{DriverManager, Connection}

import altitude.{Configuration, Altitude}
import org.slf4j.LoggerFactory
import org.sqlite.SQLiteConfig

object SqliteTransactionManager {
  private val config = new Configuration()
  val url = config.getString("db.sqlite.url")

  val sqliteConfig: SQLiteConfig = new SQLiteConfig()
  sqliteConfig.setReadOnly(true)
  private val roConnection = DriverManager.getConnection(url, sqliteConfig.toProperties)
  private val wConnection = DriverManager.getConnection(url)
  wConnection.setAutoCommit(false)
}


class SqliteTransactionManager(app: Altitude, txContainer: scala.collection.mutable.Map[Int, JdbcTransaction])
  extends JdbcTransactionManager(app, txContainer) {
  private final val log = LoggerFactory.getLogger(getClass)

  override def connection(readOnly: Boolean = false): Connection = {
    val conn = readOnly match {
      case true => SqliteTransactionManager.roConnection
      case false => SqliteTransactionManager.wConnection
    }

    log.info(s"Opening connection $conn. Read-only: $readOnly")
    conn
  }

  override def closeTransaction(tx: JdbcTransaction) = {
  }

  override def closeConnection(conn: Connection) = {
  }
}
