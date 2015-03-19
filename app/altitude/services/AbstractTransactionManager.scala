package altitude.services

abstract class AbstractTransactionManager {
  def transaction[A](f: => A): A
  def readOnly[A](f: => A): A
  def commit()
  def rollback()
}
