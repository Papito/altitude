package integration

import altitude.dao.postgres.BasePostgresDao
import altitude.services.BaseService
import org.scalatest.DoNotDiscover


@DoNotDiscover class BasePostgresDaoTests(val config: Map[String, _])
  extends IntegrationTestCore with BaseDaoTests {

  class TestPostgresDao extends BasePostgresDao("test")

  class TestPostgresService extends BaseService[TestModel] {
    override protected val DAO = new TestPostgresDao
  }

  override def service = new TestPostgresService
}
