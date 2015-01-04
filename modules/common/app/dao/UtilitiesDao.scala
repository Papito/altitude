package dao

trait UtilitiesDao extends BaseDao[Nothing] {
  def dropDatabase(): Unit
}
