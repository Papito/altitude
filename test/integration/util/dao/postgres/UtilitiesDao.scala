package integration.util.dao.postgres

import altitude.dao.postgres.BasePostgresDao

class UtilitiesDao extends BasePostgresDao("") with integration.util.dao.UtilitiesDao {
  override def dropDatabase() = Unit
}
