package altitude.transactions

import java.sql.{Connection, DriverManager}
import java.util.Properties

import altitude.{Altitude, Const => C}
import org.slf4j.LoggerFactory

class JdbcTransactionManager(val app: Altitude) extends AbstractTransactionManager {
  private final val log = LoggerFactory.getLogger(getClass)

  /*
  Get an existing transaction if we are already within a transaction context,
  else, create a new one
   */
  def transaction(implicit txId: TransactionId): JdbcTransaction = {
    log.debug(s"TX. Getting transaction for ${txId.id}")

    // see if we already have a transaction id defined
    if (app.JDBC_TRANSACTIONS.contains(txId.id)) {
      // we do, eh
      return app.JDBC_TRANSACTIONS.get(txId.id).get
    }

    // get a connection and a new transaction
    val conn: Connection = connection

    val tx: JdbcTransaction = new JdbcTransaction(conn)
    app.JDBC_TRANSACTIONS. += (tx.id -> tx)
    // assign the integer transaction ID to the mutable transaction id "carrier" object
    txId.id = tx.id
    app.transactions.CREATED += 1
    tx
  }

  def connection: Connection = app.dataSourceType match {
    case "postgres"  => {
      val props = new Properties
      val user = app.config.getString("db.postgres.user")
      props.setProperty("user", user)
      val password = app.config.getString("db.postgres.password")
      props.setProperty("password", password)
      val url = app.config.getString("db.postgres.url")
      DriverManager.getConnection(url, props)
    }
    case "sqlite" => {
      val url = app.config.getString("db.sqlite.url")
      DriverManager.getConnection(url)
    }
    case _ => throw new IllegalArgumentException("Do not know of datasource: ${altitude.dataSourceType}")
  }

  override def withTransaction[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {
    val tx = transaction

    try {
      tx.setReadOnly(flag=false)
      tx.setAutoCommit(flag=false)

      tx.up() // level up - any new transactions within will be "nested"
      val res: A = f
      tx.down()

      if (!tx.isNested) {
        log.debug(s"TX. End: ${tx.id}", C.LogTag.DB)
        tx.commit()
        app.transactions.COMMITTED += 1
      }

      res
    }
    catch {
      case ex: Exception => {
        tx.down()
        log.error("TX. Error: " + ex.getMessage)
        throw ex
      }
    }
    finally {
      // clean up, if we are done with this transaction
      if (!tx.isNested) {
        log.debug(s"TX. Closing: ${tx.id}", C.LogTag.DB)
        tx.close()
        app.transactions.CLOSED += 1
        app.JDBC_TRANSACTIONS.remove(txId.id)
      }
    }
  }

  override def asReadOnly[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {
    val tx = transaction

    try {
      if (supportsReadOnlyConnections) {
        tx.setReadOnly(flag=true)
      }

      tx.up()
      val res: A = f
      tx.down()

      res
    }
    catch {
      case ex: Exception => {
        tx.down()
        log.error("TX. Error: " + ex.getMessage)
        throw ex
      }
    }
    finally {
      if (!tx.isNested) {
        log.debug(s"TX. End: ${tx.id}", C.LogTag.DB)
        log.debug(s"TX. Closing: ${tx.id}", C.LogTag.DB)
        tx.close()
        app.transactions.CLOSED += 1
        app.JDBC_TRANSACTIONS.remove(txId.id)
      }
    }
  }

  def supportsReadOnlyConnections = app.dataSourceType match {
    case "postgres"  => true
    case _ => false
  }
}