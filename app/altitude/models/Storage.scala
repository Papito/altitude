package altitude.models

import altitude.models.StorageType.StorageType
import altitude.{Const => C}
import play.api.libs.json.{Json, JsValue}
import scala.language.implicitConversions

object Storage {
  implicit def fromJson(json: JsValue): Storage = new Storage(
    id = (json \ C.Storage.ID).as[String],
    name = (json \ C.Storage.NAME).as[String],
    description = (json \ C.Storage.DESCRIPTION).as[String],
    storageType = json \ C.Storage.TYPE
  )
}

case class Storage(override final val id: String,
                   name: String,
                   storageType: StorageType,
                   description: String = "") extends BaseModel {

  override def toJson = Json.obj(
    C.Storage.ID -> id,
    C.Storage.NAME -> name,
    C.Storage.TYPE -> storageType.toString,
    C.Storage.DESCRIPTION -> description
  )
}