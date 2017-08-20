package software.altitude.core.models

import play.api.libs.json.{JsValue, Json}
import software.altitude.core.{Const => C}

import scala.language.implicitConversions

object User {
  implicit def fromJson(json: JsValue): User = User(
    id = (json \ C.Base.ID).asOpt[String]
  ).withCoreAttr(json)
}

case class User(id: Option[String] = None) extends BaseModel {

  override def toJson = Json.obj(
    C.Base.ID -> id
  )

  override def toString = s"<username> ${id.getOrElse("NO ID")}"
}
