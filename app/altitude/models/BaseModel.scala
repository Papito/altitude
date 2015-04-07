package altitude.models

import play.api.libs.json.{JsValue, Json}
import reactivemongo.bson.BSONObjectID

import scala.language.implicitConversions

object BaseModel {
  implicit def toJson(obj: Asset): JsValue = obj.toJson
}

abstract class BaseModel(objId: Option[String] = None) {
  final val id: String = objId.getOrElse(genId)

  private final def genId: String = BSONObjectID.generate.stringify
  def toJson: JsValue = Json.obj(
    "id" -> id
  )
}
