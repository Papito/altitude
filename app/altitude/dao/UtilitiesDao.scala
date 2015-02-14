package altitude.dao

trait UtilitiesDao extends BaseDao {
  def dropDatabase(): Unit
}
