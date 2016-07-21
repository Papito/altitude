package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{JsValue, Json}
import scala.language.implicitConversions

object User {
  implicit def fromJson(json: JsValue): User = User(
    id = (json \ C("Base.ID")).asOpt[String]
  ).withCoreAttr(json)
}

case class User(id: Option[String] = None) extends BaseModel {
  override def toJson = Json.obj(
    C("Base.ID" ) -> id
  )
}
