package software.altitude.core.controllers.api.admin

import org.scalatra.BadRequest
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import software.altitude.core.{DataScrubber, ValidationException}
import software.altitude.core.Validators.ApiRequestValidator
import software.altitude.core.controllers.api.BaseApiController

class SetupController extends BaseApiController  {
  private final val log = LoggerFactory.getLogger(getClass)

  private val dataScrubber = DataScrubber(
    trim = List("repositoryName", "adminEmail", "password", "password2"),
    lower = List("adminEmail")
  )

  private val ApiRequestValidator = new ApiRequestValidator(
    required = List("repositoryName", "adminEmail", "password", "password2"),
    maxLengths = Map(
      "repositoryName" -> 80,
      "adminEmail" -> 80,
      "password" -> 50,
      "password2" -> 50
    ),
  )

  post("/") {
    if (app.isInitialized) {
      val message = "Instance is already initialized."
      log.warn(message)
      halt(400, message)
    }

    log.warn("Initializing up the instance...")

    val json = try {
      scrubAndValidatedJson(scrubber=dataScrubber, validator=ApiRequestValidator)
    }
    catch {
      case validationEx: ValidationException =>
        halt(BadRequest(
          Json.obj(
            "fieldErrors" -> validationEx.errors))
        )
    }

    val repositoryName = (json \ "repositoryName").as[String]
    val email = (json \ "adminEmail").as[String]
    val password = (json \ "password").as[String]
    val password2 = (json \ "password2").as[String]

    response.addHeader("HX-Redirect", "/")
  }

}
