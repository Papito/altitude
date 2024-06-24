package software.altitude.core.controllers.api.admin

import org.slf4j.LoggerFactory
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
    lower = List(C.Api.Fields.ADMIN_EMAIL)
  )

  private val apiRequestValidator = ApiRequestValidator(
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

    val json = dataScrubber.scrub(unscrubbedReqJson.get)

    val validationException: ValidationException = try {
      apiRequestValidator.validate(unscrubbedReqJson.get)
      ValidationException()
    }
    catch {
      case validationEx: ValidationException =>
        validationEx
      case ex: Throwable =>
        logger.error(ex.getMessage, ex)
        halt(500, "Server error")
    }

    val repositoryName = (json \ C.Api.Fields.REPOSITORY_NAME).as[String]
    val email = (json \ C.Api.Fields.ADMIN_EMAIL).as[String]
    val password = (json \ C.Api.Fields.PASSWORD).as[String]
    val password2 = (json \ C.Api.Fields.PASSWORD2).as[String]

    /*
    Continue with secondary validation checks (only if the primary validation checks have passed for these fields)
     */
    if (!validationException.errors.contains(C.Api.Fields.ADMIN_EMAIL)) {
      if (!email.contains("@")) {
        validationException.errors.addOne(C.Api.Fields.ADMIN_EMAIL -> C.Msg.Err.NOT_A_VALID_EMAIL)
      }
    }

    if (!validationException.errors.contains(C.Api.Fields.PASSWORD) &&
      !validationException.errors.contains(C.Api.Fields.PASSWORD2)) {
      if (password != password2) {
        validationException.errors.addOne(C.Api.Fields.PASSWORD -> C.Msg.Err.PASSWORDS_DO_NOT_MATCH)
      }
    }

    // if we have errors
    if (validationException.errors.nonEmpty) {
      halt(200, ssp(
          "/htmx/admin/setup_form",
          "fieldErrors" -> validationException.errors.toMap, // to immutable map
          "formJson" -> json)
      )
    }

    // On OK, send a magical header so that HTMX can redirect to the landing page
    response.addHeader("HX-Redirect", "/")
  }

}
