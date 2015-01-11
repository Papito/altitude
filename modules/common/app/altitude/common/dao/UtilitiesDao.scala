package altitude.common.dao

trait UtilitiesDao extends BaseDao[Nothing] {
  def dropDatabase(): Unit
}
