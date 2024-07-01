package software.altitude.test.core.suites

import software.altitude.core.Altitude
import software.altitude.core.{Const => C}

object ControllerSuiteBundle {
  val app = new Altitude(Map("datasource" -> C.DatasourceType.POSTGRES))
}

/*
 The web suite does NOT run against all DB engines. It only runs against Postgres..
 */
class ControllerSuiteBundle extends AllControllerTestSuites with ControllerBundleSetup
