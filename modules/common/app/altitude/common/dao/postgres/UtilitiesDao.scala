package altitude.common.dao.postgres

class UtilitiesDao extends BasePostgresDao[Nothing, Nothing]("") with altitude.common.dao.UtilitiesDao {
  override def dropDatabase(): Unit = {
  }
}
