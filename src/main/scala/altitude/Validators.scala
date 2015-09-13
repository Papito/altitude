package altitude

import altitude.exceptions.ValidationException
import org.scalatra.Params
import play.api.libs.json.{JsObject}
import altitude.{Const => C}

object Validators {

  case class Validator(required: Option[List[String]] = None,
                       maxLengths: Option[Map[String, Int]] = None) {

    def validate(json: JsObject, raise: Boolean = true): ValidationException = {
      val ex: ValidationException = new ValidationException
      checkRequired(json, ex)

      if (raise && ex.errors.nonEmpty) throw ex
      ex
    }

    protected def checkRequired(json: JsObject, ex: ValidationException): ValidationException = {
      required.getOrElse(List[String]()) foreach { field =>
        json.keys.contains(field) match {
          case false => ex.errors += (field -> C.MSG("err.required"))
          case _ =>
        }
      }
      ex
    }
  }

  case class ApiValidator(required: List[String]) {
    def validate(params: Params): Unit = {
      val ex: ValidationException = new ValidationException

      required foreach { field =>
        params.contains(field) match {
          case false => ex.errors += (field -> C.MSG("err.required"))
          case _ =>
        }
      }

      if (ex.errors.nonEmpty) throw ex
    }
  }
}
