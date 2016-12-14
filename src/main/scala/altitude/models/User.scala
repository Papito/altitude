package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{JsValue, Json}

import scala.language.implicitConversions

object User {
  implicit def fromJson(json: JsValue): User = User(
    id = (json \ C("Base.ID")).asOpt[String],
    rootFolderId = (json \ C("User.ROOT_FOLDER_ID")).as[String],
    uncatFolderId = (json \ C("User.UNCAT_FOLDER_ID")).as[String]
  ).withCoreAttr(json)
}

case class User(id: Option[String] = None,
                rootFolderId: String,
                uncatFolderId: String) extends BaseModel {
  override def toJson = Json.obj(
    C("Base.ID" ) -> id
  )
}
