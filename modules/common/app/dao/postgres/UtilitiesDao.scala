package dao.postgres

class UtilitiesDao extends BasePostgresDao[Nothing, Nothing]("") with dao.UtilitiesDao {
  override def dropDatabase(): Unit = {
  }
}
