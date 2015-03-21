package altitude.services

import altitude.util.log

class JdbcTransactionManager extends AbstractTransactionManager {
  def transaction[A](f: => A) = {
    log.info("TRANSACTION START")
    //conn.setReadOnly(false)
    //conn.setAutoCommit(false)
    val res: A = f
    this.commit()
    log.info("TRANSACTION END")
    res
  }

  def readOnly[A](f: => A) = {
    log.info("READONLY")
    f
  }

  def rollback() = {
    log.info("ROLLBACK")
  }

  def commit() = {
    log.info("COMMIT")
  }
}
