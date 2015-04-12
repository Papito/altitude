package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{Json, JsValue}
import scala.language.implicitConversions

object StorageLocation {
  implicit def fromJson(json: JsValue): StorageLocation = StorageLocation(
    storageId = (json \ C.StorageLocation.ID).as[String],
    path = (json \ C.StorageLocation.PATH).as[String]
  )
}

case class StorageLocation(storageId: String, path: String) {
  def toJson = Json.obj(
    C.StorageLocation.STORAGE_ID -> storageId,
    C.StorageLocation.PATH -> path)
}


