package altitude.models

import play.api.libs.json.{JsValue, Json}
import reactivemongo.bson.BSONObjectID
import altitude.{Const => C}

import scala.language.implicitConversions

object BaseModel {
  final def genId: String = BSONObjectID.generate.stringify
  implicit def toJson(obj: BaseModel): JsValue = obj.toJson
}

abstract class BaseModel(val id: String = BaseModel.genId) {
  def toJson: JsValue = Json.obj(
    C.Base.ID -> id
  )
}
