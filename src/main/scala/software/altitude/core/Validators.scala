package software.altitude.core

import play.api.libs.json.JsObject
import software.altitude.core.{Const => C}

object Validators {

  /**
   * Validator used in the API controller layer to validate requests.
   * This will validate that the data can be safely cast into a model,
   * at which point model validators take over.
   *
   * API layer validation is blissfully unaware of more complex
   * business-level logic.
   */
  case class ApiRequestValidator(required: Option[List[String]] = None,
                                 maxLengths: Option[Map[String, Int]] = None) {
    def validate(json: JsObject): Unit = {
      val ex: ValidationException = ValidationException()

      required.getOrElse(List()) foreach { field =>
        if (!json.keys.contains(field)) {
          ex.errors += (field -> C.Msg.Err.REQUIRED)
        }
      }

      ex.trigger()
    }
  }
}
