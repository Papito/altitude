package software.altitude.core

import play.api.libs.json.JsObject
import software.altitude.core.{Const => C}

import scala.util.matching.Regex

object Validators {
  // https://stackoverflow.com/a/32445372/53687
  private val emailRegex: Regex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  /**
   * API request validator for JSON payloads
   *
   * @param required List of required fields in the request.
   * @param maxLengths Map of fields to their maximum allowed lengths.
   * @param minLengths Map of fields to their minimum required lengths.
   */
  case class ApiRequestValidator(required: List[String] = List(),
                                 maxLengths: Map[String, Int] = Map.empty,
                                 minLengths: Map[String, Int] = Map.empty,
                                 email: List[String] = List.empty) {
    /**
     * Validates a JSON object based on the specified rules.
     *
     * @param json The JSON object to validate.
     * @throws ValidationException If the JSON object does not meet the validation criteria.
     */
    def validate(json: JsObject): Unit = {
      val ex: ValidationException = ValidationException()

      // Check for required fields
      required.foreach { field =>
        if (!json.keys.contains(field) || json(field).as[String].isEmpty) {
          ex.errors += (field -> C.Msg.Err.VALUE_REQUIRED)
        }
      }

      // Check for fields exceeding maximum length
      maxLengths foreach { case (field, maxLength) =>
        if (json.keys.contains(field) && json(field).as[String].length > maxLength) {
          ex.errors += (field -> C.Msg.Err.VALUE_TOO_LONG.format(maxLength))
        }
      }

      // Check for fields not meeting minimum length
      minLengths foreach { case (field, minLength) =>
        if (json.keys.contains(field) &&
          json(field).as[String].length < minLength) {

          ex.errors += (field -> C.Msg.Err.VALUE_TOO_SHORT.format(minLength))
        }
      }

      email.foreach { field =>
        if (!ex.errors.contains(field) && json.keys.contains(field) &&
          !emailRegex.matches(json(field).as[String])) {
          ex.errors += (field -> C.Msg.Err.VALUE_NOT_AN_EMAIL)
        }
      }

      // Trigger the exception if there are any validation errors
      ex.trigger()
    }
  }
}
