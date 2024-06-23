package software.altitude.core.controllers.api.admin

import org.scalatra.BadRequest
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import software.altitude.core.{DataScrubber, ValidationException}
import software.altitude.core.Validators.ApiRequestValidator
import software.altitude.core.controllers.api.BaseApiController
import software.altitude.core.{Const => C}

class SetupController extends BaseApiController  {
  private final val log = LoggerFactory.getLogger(getClass)

  private val dataScrubber = DataScrubber(
    trim = List(C.Api.Fields.REPOSITORY_NAME, C.Api.Fields.ADMIN_EMAIL, C.Api.Fields.PASSWORD, C.Api.Fields.PASSWORD2),
    lower = List("adminEmail")
  )

  private val ApiRequestValidator = new ApiRequestValidator(
    required = List(C.Api.Fields.REPOSITORY_NAME, C.Api.Fields.ADMIN_EMAIL, C.Api.Fields.PASSWORD, C.Api.Fields.PASSWORD2),
    maxLengths = Map(
      C.Api.Fields.REPOSITORY_NAME -> 80,
      C.Api.Fields.ADMIN_EMAIL -> 80,
      C.Api.Fields.PASSWORD -> 50,
      C.Api.Fields.PASSWORD2 -> 50
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
            C.Api.Fields.FIELD_ERRORS -> validationEx.errors))
        )
    }

    val repositoryName = (json \ C.Api.Fields.REPOSITORY_NAME).as[String]
    val email = (json \ C.Api.Fields.ADMIN_EMAIL).as[String]
    val password = (json \ C.Api.Fields.PASSWORD).as[String]
    val password2 = (json \ C.Api.Fields.PASSWORD2).as[String]

    if (password != password2) {
      halt(BadRequest(
        Json.obj(
          C.Api.Fields.FIELD_ERRORS -> Json.obj(
            C.Api.Fields.PASSWORD -> C.Msg.Err.PASSWORDS_DO_NOT_MATCH
          )))
      )
    }

    // On OK, send a magical header so that HTMX can redirect to the landing page
    response.addHeader("HX-Redirect", "/")
  }

}
