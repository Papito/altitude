package altitude.dao.postgres

class UtilitiesDao extends BasePostgresDao[Nothing, Nothing]("") with altitude.dao.UtilitiesDao {
  override def dropDatabase() = Unit
}
