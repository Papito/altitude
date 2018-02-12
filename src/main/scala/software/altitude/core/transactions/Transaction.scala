package software.altitude.core.transactions

trait Transaction {
  val id: Int = scala.util.Random.nextInt(java.lang.Integer.MAX_VALUE)
  val isReadOnly: Boolean
  protected var _level: Int  = 0
  def level: Int = _level
  def hasParents: Boolean = level > 0
  def mustCommit: Boolean = !hasParents

  def up(): Unit = _level = _level + 1
  def down(): Unit = _level = if (_level > 0) _level - 1 else 0
  def close(): Unit
  def commit(): Unit
  def rollback(): Unit
}
