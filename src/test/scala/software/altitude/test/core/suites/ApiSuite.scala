package software.altitude.test.core.suites

import software.altitude.core.{Const =>C, Altitude}
import software.altitude.test.core.api.AssetEndpointTests


object ApiSuite {
  val app = new Altitude(Map("datasource" ->C.DatasourceType.SQLITE))
}

class ApiSuite extends SqliteSuiteSetup {
  val config = ApiSuite.app.configOverride
  new AssetEndpointTests(config)
}
