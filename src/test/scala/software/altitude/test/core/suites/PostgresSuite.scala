package software.altitude.test.core.suites


import software.altitude.core.{Altitude, Const => C}

object PostgresSuite {
  val app = new Altitude(Map("datasource" ->C.DatasourceType.POSTGRES))
}

class PostgresSuite
  extends AllIntegrationTests(config = Map("datasource" -> C.DatasourceType.POSTGRES))
  with PostgresSuiteSetup