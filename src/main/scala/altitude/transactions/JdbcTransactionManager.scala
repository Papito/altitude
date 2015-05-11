package altitude.transactions

import altitude.{Const => C, Altitude}
import org.slf4j.LoggerFactory

class JdbcTransactionManager(val app: Altitude) extends AbstractTransactionManager {
  val log =  LoggerFactory.getLogger(getClass)

  def transaction(implicit txId: TransactionId): JdbcTransaction = {
    if (app.JDBC_TRANSACTIONS.contains(txId.id)) {
      return app.JDBC_TRANSACTIONS.get(txId.id).get
    }
    val tx: JdbcTransaction = new JdbcTransaction
    app.JDBC_TRANSACTIONS. += (tx.id -> tx)
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

      log.debug(s"TRANSACTION END: ${tx.id}", C.tag.DB)
      tx.commit()

      res
    }
    finally {
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