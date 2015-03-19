package altitude.services

import altitude.util.log

class VoidTransactionManager extends AbstractTransactionManager {
  def transaction[A](f: => A) = {
    log.info("TRANSACTION START")
    val res = f
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
