package software.altitude.core

import play.api.libs.json.JsObject
import software.altitude.core.{Const => C}

object Validators {

  /**
   * Base model validator implementation
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

    private def checkRequired(json: JsObject, ex: ValidationException): ValidationException = {
      required.getOrElse(List[String]()) foreach { field =>
        if (json.keys.contains(field)) {
          (json \ field).asOpt[String] match {
            // see if the value is an empty string
            case Some("") => ex.errors += (field -> C.Msg.Err.REQUIRED)
            case _ =>
          }
        }
        else {
          ex.errors += (field -> C.Msg.Err.REQUIRED)
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
        if (!json.keys.contains(field)) {
          ex.errors += (field -> C.Msg.Err.REQUIRED)
        }
      }

      ex.trigger()
    }
  }
}
