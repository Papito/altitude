package software.altitude.core

import org.slf4j.LoggerFactory

class AltitudeCoreApp(configOverride: Map[String, Any] = Map()) {
  private final val log = LoggerFactory.getLogger(getClass)

  // ID for this application - which we may have multiple of in the same environment
  final val id = scala.util.Random.nextInt(java.lang.Integer.MAX_VALUE)

  // this is the environment we are running in
  final val environment = Environment.ENV match {
    case Environment.DEV => "development"
    case Environment.PROD => "production"
    case Environment.TEST => "test"
  }
  log.info(s"Environment is: $environment")

  final val config = new Configuration(
    configOverride = configOverride)

  final val dataSourceType = config.datasourceType
  log.info(s"Datasource type: $dataSourceType")

}
