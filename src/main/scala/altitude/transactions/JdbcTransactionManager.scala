package altitude.transactions

import java.sql.{Connection, DriverManager}
import java.util.Properties

import altitude.{Altitude, Const => C}
import org.slf4j.LoggerFactory

class JdbcTransactionManager(val app: Altitude) extends AbstractTransactionManager {
  val log =  LoggerFactory.getLogger(getClass)

  /*
  Get an existing transaction if we are already withing a transaction context,
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

    val props = new Properties
    val user = app.config.getString("db.postgres.user")
    props.setProperty("user", user)
    val password = app.config.getString("db.postgres.password")
    props.setProperty("password", password)
    val url = app.config.getString("db.postgres.url")
    val conn: Connection = DriverManager.getConnection(url, props)

    val tx: JdbcTransaction = new JdbcTransaction(conn)
    app.JDBC_TRANSACTIONS. += (tx.id -> tx)
    // assign the integer transaction ID to the mutable transaction id "carrier" object
    txId.id = tx.id
    tx
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
        log.debug(s"TRANSACTION END: ${tx.id}", C.tag.DB)
        tx.commit()
      }

      res
    }
    finally {
      // clean up, if we are done with this transaction
      if (!tx.isNested) {
        tx.close()
        app.JDBC_TRANSACTIONS.remove(txId.id)
      }
    }
  }

  override def asReadOnly[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {
    val tx = transaction

    try {
      tx.setReadOnly(flag=true)

      tx.up()
      val res: A = f
      tx.down()

      res
    }
    finally {
      if (!tx.isNested) {
        tx.close()
        app.JDBC_TRANSACTIONS.remove(txId.id)
      }
    }
  }
}