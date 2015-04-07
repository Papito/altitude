package altitude.services

import altitude.dao.{JdbcTransaction, TransactionId}
import altitude.util.log
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

  def withTransaction[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {

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

  def asReadOnly[A](f: => A)(implicit txId: TransactionId = new TransactionId) = {
    val tx = JdbcTransactionManager.transaction

    try {
      if (!tx.isNested){
        tx.setReadOnly(flag=true)
      }

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