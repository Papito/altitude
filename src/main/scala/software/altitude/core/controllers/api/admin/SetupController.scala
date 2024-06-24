package software.altitude.core.controllers.api.admin

import org.scalatra.BadRequest
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import software.altitude.core.{DataScrubber, ValidationException, Const => C}
import software.altitude.core.Validators.ApiRequestValidator
import software.altitude.core.controllers.api.BaseApiController

class SetupController extends BaseApiController  {
  private final val log = LoggerFactory.getLogger(getClass)

  private val dataScrubber = DataScrubber(
    trim = List(
      C.Api.Fields.REPOSITORY_NAME,
      C.Api.Fields.ADMIN_EMAIL,
      C.Api.Fields.PASSWORD,
      C.Api.Fields.PASSWORD2),
    lower = List("adminEmail")
  )

  private val ApiRequestValidator = new ApiRequestValidator(
    required = List(
      C.Api.Fields.REPOSITORY_NAME,
      C.Api.Fields.ADMIN_EMAIL,
      C.Api.Fields.PASSWORD,
      C.Api.Fields.PASSWORD2),
    maxLengths = Map(
      C.Api.Fields.REPOSITORY_NAME -> C.Api.Constraints.MAX_REPOSITORY_NAME_LENGTH,
      C.Api.Fields.ADMIN_EMAIL -> C.Api.Constraints.MAX_EMAIL_LENGTH,
      C.Api.Fields.PASSWORD -> C.Api.Constraints.MAX_PASSWORD_LENGTH,
    ),
    minLengths = Map(
      C.Api.Fields.REPOSITORY_NAME -> C.Api.Constraints.MIN_REPOSITORY_NAME_LENGTH,
      C.Api.Fields.ADMIN_EMAIL -> C.Api.Constraints.MIN_EMAIL_LENGTH,
      C.Api.Fields.PASSWORD -> C.Api.Constraints.MIN_PASSWORD_LENGTH,
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
        halt(200, mustache(
          "/htmx/admin/setup_form",
          "fields" -> C.Api.Fields,
          "constr" -> C.Api.Constraints,
          "field_errors" -> validationErrorsForMustache(validationEx))
        )
    }


    val repositoryName = (json \ C.Api.Fields.REPOSITORY_NAME).as[String]
    val email = (json \ C.Api.Fields.ADMIN_EMAIL).as[String]
    val password = (json \ C.Api.Fields.PASSWORD).as[String]
    val password2 = (json \ C.Api.Fields.PASSWORD2).as[String]

    if (!email.contains("@")) {
      halt(BadRequest(
        Json.obj(
          C.Api.Fields.FIELD_ERRORS -> Json.obj(
            C.Api.Fields.ADMIN_EMAIL -> C.Msg.Err.NOT_A_VALID_EMAIL
          )))
      )

    }

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
