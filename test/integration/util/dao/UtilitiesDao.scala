package integration.util.dao

import altitude.dao.BaseDao

trait UtilitiesDao extends BaseDao {
  def dropDatabase(): Unit
}
