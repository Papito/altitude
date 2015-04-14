package altitude.models

import altitude.models.StorageType.StorageType
import altitude.{Const => C}
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json, JsValue}
import scala.language.implicitConversions

object Storage {
  implicit def fromJson(json: JsValue): Storage = new Storage(
    id = (json \ C.Storage.ID).asOpt[String],
    name = (json \ C.Storage.NAME).as[String],
    description = (json \ C.Storage.DESCRIPTION).as[String],
    storageType = json \ C.Storage.TYPE
  )
}

case class Storage(id: Option[String] = None,
                   name: String,
                   storageType: StorageType,
                   description: String = "") extends BaseModel {

  override def toJson = Json.obj(
    C.Storage.NAME -> name,
    C.Storage.TYPE -> storageType.toString,
    C.Storage.DESCRIPTION -> description
  ) ++ coreAttrs
}