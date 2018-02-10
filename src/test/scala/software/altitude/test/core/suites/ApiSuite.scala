package software.altitude.test.core.suites

import software.altitude.core.{Const =>C, Altitude}
import software.altitude.test.core.api.AssetEndpointTests


class ApiSuite extends SqliteSuiteSetup {
  new AssetEndpointTests
}
