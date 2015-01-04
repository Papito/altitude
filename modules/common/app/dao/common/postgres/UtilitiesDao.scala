package dao.common.postgres

class UtilitiesDao extends BasePostgresDao[Nothing, Nothing]("") with dao.common.UtilitiesDao {
  override def dropDatabase(): Unit = {
  }
}
