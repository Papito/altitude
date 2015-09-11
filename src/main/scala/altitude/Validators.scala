package altitude

import altitude.exceptions.ValidationException
import play.api.libs.json.{JsObject}
import altitude.{Const => C}

object Validators {

  case class Validator(required: Option[List[String]] = None,
                       maxLengths: Option[Map[String, Int]] = None) {

    def validate(json: JsObject): Unit = {
      val exception = new ValidationException
      checkRequired(json, exception)

      if (exception.errors.nonEmpty) {
        throw exception
      }
    }

    private def checkRequired(json: JsObject, ex: ValidationException): Unit = {
      required.getOrElse(List[String]()) foreach { field =>
        (json \ field).asOpt[String] match {
          case v if v.isEmpty || v.get == "" => ex.errors += (field -> C.MSG("err.required"))
        }
      }
    }
  }
}
