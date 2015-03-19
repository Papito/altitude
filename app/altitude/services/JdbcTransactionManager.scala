package altitude.services

import altitude.util.log

class JdbcTransactionManager extends AbstractTransactionManager {
  def transaction[A](f: => A) = {
    log.info("TRANSACTION START")
    val res: A = f
    this.commit()
    log.info("TRANSACTION END")
    res
  }

  def readOnly[A](f: => A) = {
    log.info("READONLY")
    f
  }

  def rollback[A](f: => A) = {
    log.info("ROLLBACK")
  }

  def commit = {
    log.info("COMMIT")
  }
}
