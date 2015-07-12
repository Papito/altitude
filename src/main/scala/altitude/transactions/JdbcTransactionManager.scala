package altitude.transactions

import java.sql.{Connection, DriverManager}
import java.util.Properties

import altitude.{Altitude, Const => C}
import org.slf4j.LoggerFactory

object JdbcTransactionManager {
  val TRANSACTIONS = scala.collection.mutable.Map[Int, JdbcTransaction]()
}

class JdbcTransactionManager(val app: Altitude) extends AbstractTransactionManager {
  val log =  LoggerFactory.getLogger(getClass)

  def transaction(implicit txId: TransactionId): JdbcTransaction = {
    // see if we already have a transaction id defined
    if (JdbcTransactionManager.TRANSACTIONS.contains(txId.id)) {
      // we do, eh
      return JdbcTransactionManager.TRANSACTIONS.get(txId.id).get
    }

    // create a connection and a transaction

    val props = new Properties
    val user = app.config.get("db.postgres.user")
    props.setProperty("user", user)
    val password = app.config.get("db.postgres.password")
    props.setProperty("password", password)
    val url = app.config.get("db.postgres.url")
    val conn: Connection = DriverManager.getConnection(url, props)

    val tx: JdbcTransaction = new JdbcTransaction(conn)
    JdbcTransactionManager.TRANSACTIONS. += (tx.id -> tx)
    // assign the integer transaction to the mutable transaction id "carrier" object
    txId.id = tx.id
    tx
  }

  override def withTransaction[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {
    val tx = transaction

    try {
      tx.setReadOnly(flag=false)
      tx.setAutoCommit(flag=false)

      tx.up()
      val res: A = f
      tx.down()

      if (!tx.isNested) {
        log.debug(s"TRANSACTION END: ${tx.id}", C.tag.DB)
        tx.commit()
      }

      res
    }
    finally {
      if (!tx.isNested) {
        tx.close()
        JdbcTransactionManager.TRANSACTIONS.remove(txId.id)
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
        JdbcTransactionManager.TRANSACTIONS.remove(txId.id)
      }
    }
  }
}