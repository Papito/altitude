package altitude.dao.postgres

class UtilitiesDao extends BasePostgresDao("") with altitude.dao.UtilitiesDao {
  override def dropDatabase() = Unit
}
