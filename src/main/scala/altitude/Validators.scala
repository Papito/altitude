package altitude

import altitude.exceptions.ValidationException
import altitude.models.BaseModel
import play.api.libs.json.JsValue
import altitude.{Const => C}

object Validators {

  class Validator(val required: Option[List[String]] = None, val maxLengths: Option[Map[String, Int]] = None) {
    val exception = new ValidationException

    def validate(model: BaseModel): Unit = {
      val json = model.toJson
      if (required.isDefined) checkRequired(json)

      if (exception.errors.nonEmpty) {
        throw new ValidationException
      }
    }

    private def checkRequired(json: JsValue): Unit = {
      required.get.foreach { field =>
        val value = (json \ field).asOpt[String]
        if (value.isEmpty || value.get == "") {
          exception.errors += (field -> C.MSG("err.required"))
        }
      }
    }
  }
}
