package altitude.services

import altitude.{Const => C}

object JdbcTransactionManager {
  val TRANSACTIONS = scala.collection.mutable.Map[Int, JdbcTransaction]()

  def transaction(implicit txId: TransactionId): JdbcTransaction = {
    if (TRANSACTIONS.contains(txId.id)) {
      return TRANSACTIONS.get(txId.id).get
    }
    val tx: JdbcTransaction = new JdbcTransaction
    TRANSACTIONS += (tx.id -> tx)
    txId.id = tx.id
    tx
  }
}

class JdbcTransactionManager extends AbstractTransactionManager {

  override def withTransaction[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {

    val tx = JdbcTransactionManager.transaction

    try {
      tx.setReadOnly(flag=false)
      tx.setAutoCommit(flag=false)

      tx.up()
      val res: A = f
      tx.down()

      log.debug(s"TRANSACTION END: ${tx.id}", C.LogTag.DB)
      tx.commit()

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
    val tx = JdbcTransactionManager.transaction

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