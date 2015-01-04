package dao.common

trait UtilitiesDao extends BaseDao[Nothing] {
  def dropDatabase(): Unit
}
