package software.altitude.core.transactions

import java.sql.{Connection, DriverManager}
import java.util.concurrent.locks.ReentrantLock

import org.slf4j.LoggerFactory
import org.sqlite.SQLiteConfig
import software.altitude.core.{AltitudeCoreApp, Configuration}

object SqliteTransactionManager {
  private val config = new Configuration()
  val url = config.getString("db.sqlite.url")
  private val LOCK = new ReentrantLock()

  val sqliteConfig: SQLiteConfig = new SQLiteConfig()
  sqliteConfig.setReadOnly(true)
  private lazy val roConnection = DriverManager.getConnection(url, sqliteConfig.toProperties)
  private lazy val wConnection = DriverManager.getConnection(url)
  wConnection.setAutoCommit(false)
}

class SqliteTransactionManager(app: AltitudeCoreApp)
  extends JdbcTransactionManager(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  override def connection(readOnly: Boolean = false): Connection = {
    val conn = readOnly match {
      case true => SqliteTransactionManager.roConnection
      case false => SqliteTransactionManager.wConnection
    }

    log.debug(s"Getting connection $conn. Read-only: $readOnly")
    conn
  }

  override def lock(tx: Transaction): Unit = {
    if (tx.hasParents) {
      // already inside a transaction
      return
    }

    log.debug("Acquiring SQLite write lock")
    SqliteTransactionManager.LOCK.lock()
  }

  override def unlock(tx: Transaction): Unit = {
    if (tx.hasParents) {
      // already inside a transaction
      return
    }

    log.debug("releasing SQLite write lock")
    SqliteTransactionManager.LOCK.unlock()
  }

  override def closeTransaction(tx: JdbcTransaction) = {}
  override def closeConnection(conn: Connection) = {}
}
