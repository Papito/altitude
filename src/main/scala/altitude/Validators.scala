package altitude

import altitude.exceptions.ValidationException
import altitude.models.BaseModel
import play.api.libs.json.JsValue
import altitude.{Const => C}

object Validators {

  class Validator(val required: Option[List[String]] = None, val maxLengths: Option[Map[String, Int]] = None) {

    def validate(model: BaseModel): Unit = {
      val exception = new ValidationException
      val json = model.toJson
      if (required.isDefined) checkRequired(json, exception)

      if (exception.errors.nonEmpty) {
        throw new ValidationException
      }
    }

    private def checkRequired(json: JsValue, ex: ValidationException): Unit = {
      required.get.foreach { field =>
        val value = (json \ field).asOpt[String]
        if (value.isEmpty || value.get == "") {
          ex.errors += (field -> C.MSG("err.required"))
        }
      }
    }
  }
}
