package altitude.services

abstract class AbstractTransactionManager {
  def withTransaction[A](f: => A)(implicit txId: Int): A
  def asReadOnly[A](f: => A)(implicit txId: Int): A
}
