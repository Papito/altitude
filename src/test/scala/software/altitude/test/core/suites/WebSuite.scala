package software.altitude.test.core.suites

import software.altitude.core.Altitude
import software.altitude.core.{Const => C}

object WebSuite {
  val app = new Altitude(Map("datasource" -> C.DatasourceType.POSTGRES))
}

class WebSuite
  extends AllWebTests(config = Map("datasource" -> C.DatasourceType.POSTGRES))
  with WebSuiteSetup {
}
