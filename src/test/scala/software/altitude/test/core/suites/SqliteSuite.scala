package software.altitude.test.core.suites

import software.altitude.core.{Altitude, Const => C}

object SqliteSuite {
  val app = new Altitude(Map("datasource" ->C.DatasourceType.SQLITE))
}

class SqliteSuite
  extends AllTests(config = Map("datasource" -> C.DatasourceType.SQLITE))
  with SqliteSuiteSetup