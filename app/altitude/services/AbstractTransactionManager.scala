package altitude.services

abstract class AbstractTransactionManager {
  def transaction[A](f: => A): A
  def readOnly[A](f: => A): A
  def commit[A](f: => A): A
  def rollback[A](f: => A): A
}
