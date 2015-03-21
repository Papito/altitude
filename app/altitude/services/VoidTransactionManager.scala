package altitude.services

class VoidTransactionManager extends AbstractTransactionManager {
  def transaction[A](f: => A) = f

  def readOnly[A](f: => A) = f

  def rollback() = Unit

  def commit() = Unit
}
