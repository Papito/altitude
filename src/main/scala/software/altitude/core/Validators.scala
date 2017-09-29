package software.altitude.core

import play.api.libs.json.JsObject
import software.altitude.core.{Const => C}

object Validators {

  /**
   * Base model validator implementation that is used by services.
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
   * This will validate that the data can be safely cast into a model,
   * at which point model validators take over.
   *
   * API layer validation is blissfully unaware of more complex
   * business-level logic.
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

      ex.trigger()
    }
  }
}