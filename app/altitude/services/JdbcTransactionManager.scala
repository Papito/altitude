package altitude.services

import altitude.dao.JdbcTransaction
import altitude.util.log

object JdbcTransactionManager {
  private val TRANSACTIONS = scala.collection.mutable.Map[Int, JdbcTransaction]()

  def transaction(implicit txId: Int = 0): JdbcTransaction = {
    if (TRANSACTIONS.contains(txId)) {
      return TRANSACTIONS.get(txId).get
    }
    val tx: JdbcTransaction = new JdbcTransaction
    TRANSACTIONS += (tx.id -> tx)
    tx
  }
}

class JdbcTransactionManager extends AbstractTransactionManager {

  def withTransaction[A](f: => A)(implicit txId: Int = 0) = {
    log.debug("TRANSACTION START")

    val tx = JdbcTransactionManager.transaction

    if (tx.isNested)
      log.debug("Nested transaction: " + tx.id)

    try {
      if (!tx.isNested) {
        tx.setReadOnly(flag=false)
        tx.setAutoCommit(flag=false)
      }

      tx.up()
      val res: A = f
      tx.down()

      // commit if this is not an existing transaction
      if (!tx.isNested) tx.commit()
      log.debug("TRANSACTION END: " + tx.id)

      res
    } finally {
      if (!tx.isNested) {
        tx.close()
        JdbcTransactionManager.TRANSACTIONS.remove(txId)
      }
    }
  }

  def asReadOnly[A](f: => A)(implicit txId: Int = 0) = {
    log.debug("READONLY TRANSACTION START")

    val tx = JdbcTransactionManager.transaction

    if (tx.isNested)
      log.debug("Nested transaction: " + tx.id)
    else
      log.debug("New transaction: " + tx.id)

    try {
      if (!tx.isNested){
        tx.setReadOnly(flag=true)
      }

      tx.up()
      val res: A = f
      tx.down()

      log.debug("READONLY TRANSACTION END: " + tx.id)

      res
    } finally {
      if (!tx.isNested) {
        tx.close()
        JdbcTransactionManager.TRANSACTIONS.remove(txId)
      }
    }
  }
}