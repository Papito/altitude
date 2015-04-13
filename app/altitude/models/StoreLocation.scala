package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{Json, JsValue}
import scala.language.implicitConversions

object StoreLocation {
  implicit def fromJson(json: JsValue): StoreLocation = StoreLocation(
    storageId = (json \ C.StoreLocation.STORAGE_ID).as[String],
    path = (json \ C.StoreLocation.PATH).as[String]
  )
}

case class StoreLocation(storageId: String, path: String) extends BaseModel(id = None) {
  override def toJson = Json.obj(
    C.StoreLocation.STORAGE_ID -> storageId,
    C.StoreLocation.PATH -> path)
}


