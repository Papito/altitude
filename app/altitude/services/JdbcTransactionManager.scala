package altitude.services

import altitude.dao.{JdbcTransaction, TransactionId}
import altitude.util.log

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

  def withTransaction[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {
    log.debug("TRANSACTION START")

    val tx = JdbcTransactionManager.transaction

    try {
      if (!tx.isNested) {
        tx.setReadOnly(flag=false)
        tx.setAutoCommit(flag=false)
      }

      tx.up()
      val res: A = f
      tx.down()

      // commit if this is not an existing transaction
      if (!tx.isNested) {
        log.debug("TRANSACTION END: " + tx.id)
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

  def asReadOnly[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {
    log.debug("READONLY TRANSACTION START")

    val tx = JdbcTransactionManager.transaction

    try {
      if (!tx.isNested){
        tx.setReadOnly(flag=true)
      }

      tx.up()
      val res: A = f
      tx.down()

      log.debug("READONLY TRANSACTION END: " + tx.id)

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