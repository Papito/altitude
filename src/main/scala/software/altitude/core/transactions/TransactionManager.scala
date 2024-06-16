package software.altitude.core.transactions

import org.slf4j.LoggerFactory
import org.sqlite.SQLiteConfig
import software.altitude.core.AltitudeCoreApp
import software.altitude.core.{Const => C}

import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

class TransactionManager(val app: AltitudeCoreApp) {

  object transactions {
    var CREATED = 0
    var COMMITTED = 0
    var CLOSED = 0

    def reset(): Unit = {
      CREATED = 0
      COMMITTED = 0
      CLOSED = 0
    }
  }

  private final val log = LoggerFactory.getLogger(getClass)

  /**
   * This transaction registry is the heart of it all - we use it to keep track of existing
   * transactions, creating new ones, and cleaning up after a transaction ends.
   */
  val txRegistry: scala.collection.mutable.Map[Int, JdbcTransaction] =
    scala.collection.mutable.Map[Int, JdbcTransaction]()

  /**
   *  Get an existing transaction if we are already within a transaction context, else,
   *  create a new transaction - AND a JDBC connection
   */
  def transaction(readOnly: Boolean = false)(implicit txId: TransactionId): JdbcTransaction = {
    log.debug(s"Getting transaction for ${txId.id}")

    // see if we already have a transaction ID defined
    if (txRegistry.contains(txId.id)) {
      // we do - do nothing, return the transaction
      return txRegistry(txId.id)
    }

    // get a connection and a new transaction, read-only if so requested
    val conn: Connection = connection(readOnly)

    val tx: JdbcTransaction = new JdbcTransaction(conn, readOnly)

    // add this to our transaction registry
    txRegistry += (tx.id -> tx)

    // assign the integer transaction ID to the mutable transaction id "carrier" object
    txId.id = tx.id
    log.debug(s"CREATING TRANSACTION ${txId.id}")
    transactions.CREATED += 1
    tx
  }

  def closeTransaction(tx: JdbcTransaction): Unit = {
    tx.close()
  }

  def connection(readOnly: Boolean): Connection = {
    app.config.datasourceType match {
      case C.DatasourceType.POSTGRES =>
        val props = new Properties
        val user = app.config.getString("db.postgres.user")
        props.setProperty("user", user)
        val password = app.config.getString("db.postgres.password")
        props.setProperty("password", password)
        val url = app.config.getString("db.postgres.url")
        val conn = DriverManager.getConnection(url, props)
        log.debug(s"Opening connection $conn. Read-only: $readOnly")

        if (readOnly) {
          conn.setReadOnly(true)
        }
        else {
          conn.setReadOnly(false)
          conn.setAutoCommit(false)
        }

        conn

      case C.DatasourceType.SQLITE =>
        val url: String = app.config.getString("db.sqlite.url")

        val sqliteConfig: SQLiteConfig = new SQLiteConfig()
        sqliteConfig.setReadOnly(true)
        if (readOnly) {
          DriverManager.getConnection(url, sqliteConfig.toProperties)
        } else {
          val writeConnection = DriverManager.getConnection(url)
          writeConnection.setAutoCommit(false)

          writeConnection
        }
    }
  }

  def withTransaction[A](f: => A)(implicit txId: TransactionId = new TransactionId): A = {
    log.debug("WRITE transaction")
    val tx = transaction()

    if (tx.isReadOnly) {
      throw new IllegalStateException("The parent for this transaction is read-only!")
    }

    try {
      // level up - any new transactions within will be "nested" and not committed
      tx.up()

      // actual function call
      val res: A = f

      // level down - if the transaction is not nested after this - it will be committed
      tx.down()

      // commit exiting transactions
      tx.commit()
      if (tx.mustCommit) {
        transactions.COMMITTED += 1
      }

      res
    }
    catch {
      case ex: Exception =>
        tx.down()
        log.error(s"Error (${ex.getClass.getName}): ${ex.getMessage}")
        tx.rollback()
        throw ex
    }
    finally {
      // clean up, if we are done with this transaction
      if (!tx.hasParents) {
        log.debug(s"Closing: ${tx.id}")
        txRegistry.remove(txId.id)
        closeTransaction(tx)
        transactions.CLOSED += 1
      }
    }
  }

  def asReadOnly[A](f: => A)(implicit txId: TransactionId = new TransactionId): A = {
    log.debug("READ transaction")
    val tx = transaction(readOnly = true)

    try {
      tx.up()  // level up - any new transactions within will be "nested" and use the same connection
      f
    }
    catch {
      case ex: Exception =>
        log.error(s"Error (${ex.getClass.getName}): ${ex.getMessage}")
        throw ex
    }
    finally {
      // unlike write transactions, we can level down here, as we are not committing read-only connections
      tx.down()

      if (!tx.hasParents) {
        log.debug(s"End: ${tx.id}")
        log.debug(s"Closing: ${tx.id}")
        txRegistry.remove(txId.id)
        closeTransaction(tx)
        transactions.CLOSED += 1
      }
    }
  }

  def savepoint()(implicit txId: TransactionId): Unit = {
    transaction().addSavepoint()
  }
}
