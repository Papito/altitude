package altitude.dao

object Transaction {
  var COMMITTED = 0
  var CREATED = 0
  var CLOSED = 0
  var ROLLED_BACK = 0
}

trait Transaction {
  val id = scala.util.Random.nextInt(java.lang.Integer.MAX_VALUE)
  var _level: Int  = 0
  def level = _level
  def isNested: Boolean = level > 0

  def up() = _level = _level + 1
  def down() = _level = if (_level > 0) _level - 1 else 0
  def close(): Unit
  def commit(): Unit
  def rollback(): Unit

}
