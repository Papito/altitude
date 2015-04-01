package altitude.dao

import altitude.util.log

trait Transaction {
  val id = scala.util.Random.nextInt(java.lang.Integer.MAX_VALUE)
  var _level: Int  = 0
  def level = _level

  log.debug(s"New transaction $id")

  def isNested: Boolean = level > 0

  def up() = _level = _level + 1
  def down() = _level = _level - 1
  def close(): Unit
  def commit(): Unit
  def rollback(): Unit
  def setReadOnly(flag: Boolean): Unit
  def setAutoCommit(flag: Boolean): Unit

}
