package altitude.transactions

import java.sql.{Connection, DriverManager}
import java.util.Properties

import altitude.{Altitude, Const => C}
import org.slf4j.LoggerFactory

class JdbcTransactionManager(val app: Altitude, val txContainer: scala.collection.mutable.Map[Int, JdbcTransaction]) extends AbstractTransactionManager {
  private final val log = LoggerFactory.getLogger(getClass)

  /*
  Get an existing transaction if we are already within a transaction context,
  else, create a new one
   */
  def transaction(implicit txId: TransactionId, readOnly: Boolean = false): JdbcTransaction = {
    log.debug(s"TX. Getting transaction for ${txId.id}")

    // see if we already have a transaction id defined
    if (txContainer.contains(txId.id)) {
      // we do, eh
      return txContainer.get(txId.id).get
    }

    // get a connection and a new transaction
    val conn: Connection = connection(readOnly)

    val tx: JdbcTransaction = new JdbcTransaction(conn)
    txContainer += (tx.id -> tx)
    // assign the integer transaction ID to the mutable transaction id "carrier" object
    txId.id = tx.id
    app.transactions.CREATED += 1
    tx
  }

  def closeConnection(conn: Connection): Unit = {
    log.info(s"Closing connection $conn")
    conn.close()
  }

  protected def closeTransaction(tx: JdbcTransaction) = {
    tx.close()
  }

  def connection(readOnly: Boolean): Connection = {
    val props = new Properties
    val user = app.config.getString("db.postgres.user")
    props.setProperty("user", user)
    val password = app.config.getString("db.postgres.password")
    props.setProperty("password", password)
    val url = app.config.getString("db.postgres.url")
    val conn = DriverManager.getConnection(url, props)
    log.info(s"Opening connection $conn. Read-only: $readOnly")

    readOnly match {
      case true => conn.setReadOnly(true)
      case false =>  {
        conn.setReadOnly(false)
        conn.setAutoCommit(false)
      }
    }

    conn
  }

  override def withTransaction[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {
    val tx = transaction(txId, readOnly = false)

    try {
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
        closeTransaction(tx)
        app.transactions.CLOSED += 1
        txContainer.remove(txId.id)
      }
    }
  }

  override def asReadOnly[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {
    val tx = transaction(txId, readOnly = true)

    try {
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
        closeTransaction(tx)
        app.transactions.CLOSED += 1
        txContainer.remove(txId.id)
      }
    }
  }

}