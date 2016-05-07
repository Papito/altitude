package altitude

import altitude.exceptions.ValidationException
import altitude.{Const => C}
import org.scalatra.Params
import play.api.libs.json.JsObject

object Validators {

  /*
  WEB VALIDATOR
   */
  case class Validator(required: Option[List[String]] = None,
                       maxLengths: Option[Map[String, Int]] = None) {

    def validate(json: JsObject, raise: Boolean = true): ValidationException = {
      if (!(json \ C("Base.IS_CLEAN")).asOpt[Boolean].contains(true)) {
        throw new RuntimeException("Object not sanitized for validation")
      }

      val ex: ValidationException = ValidationException()
      checkRequired(json, ex)

      if (raise && ex.errors.nonEmpty) throw ex
      ex
    }

    protected def checkRequired(json: JsObject, ex: ValidationException): ValidationException = {
      required.getOrElse(List[String]()) foreach { field =>
        json.keys.contains(field) match {
          // see of the value is defined
          case false => ex.errors += (field -> C("msg.err.required"))
          case _ => {
            (json \ field).asOpt[String] match {
              // see if the value is an empty string
              case Some("") => ex.errors += (field -> C("msg.err.required"))
              case _ =>
            }
          }
        }
      }
      ex
    }
  }

  /*
  API VALIDATOR
   */
  case class ApiValidator(required: List[String]) {
    def validateForm(params: Params): Unit = {
      val ex: ValidationException = ValidationException()

      required foreach { field =>
        params.contains(field) match {
          case false => ex.errors += (field -> C("msg.err.required"))
          case _ =>
        }
      }

      if (ex.errors.nonEmpty) throw ex
    }

    def validate(json: JsObject): Unit = {
      // FIXME: yeah, need this
    }
  }
}