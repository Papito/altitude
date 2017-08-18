package software.altitude.core

import software.altitude.core.{Const => C}
import play.api.libs.json.JsObject

object Validators {

  /**
   * Base validator implementation that is used by services.
   * This makes sure that that JSON passed in - to be converted into a model -
   * has everything required and set in order to make the conversion valid.
   */
  case class ModelDataValidator(required: Option[List[String]] = None,
                       maxLengths: Option[Map[String, Int]] = None) {

    def validate(json: JsObject, raise: Boolean = true): ValidationException = {
      if (!(json \ C.Base.IS_CLEAN).asOpt[Boolean].contains(true)) {
        throw new RuntimeException("Object not sanitized for validation")
      }

      val ex: ValidationException = ValidationException()
      checkRequired(json, ex)

      if (raise && ex.nonEmpty) throw ex
      ex
    }

    protected def checkRequired(json: JsObject, ex: ValidationException): ValidationException = {
      required.getOrElse(List[String]()) foreach { field =>
        json.keys.contains(field) match {
          // see of the value is defined
          case false => ex.errors += (field -> C.Msg.Warn.REQUIRED)
          case _ => (json \ field).asOpt[String] match {
            // see if the value is an empty string
            case Some("") => ex.errors += (field -> C.Msg.Warn.REQUIRED)
            case _ =>
          }
        }
      }
      ex
    }
  }

  /**
   * Validator used in the API controller layer to validate requests.
   * It's not a definitive validation - it will just make sure the request
   * is not missing obvious requirements. It's still up to model data
   * validators to run a comprehensive validation of that. The services
   * are better equipped to do that, as doing this here would be redundant.
   */
  case class ApiRequestValidator(required: List[String]) {
    def validate(json: JsObject): Unit = {
      val ex: ValidationException = ValidationException()

      required foreach { field =>
        json.keys.contains(field) match {
          case false => ex.errors += (field -> C.Msg.Warn.REQUIRED)
          case _ =>
        }
      }

      if (ex.nonEmpty) throw ex
    }
  }
}