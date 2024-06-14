package software.altitude.core.models

import play.api.libs.json.{JsObject, JsValue, Json}
import software.altitude.core.{Const => C}

import scala.language.implicitConversions

object User {
  implicit def fromJson(json: JsValue): User = User(
    id = (json \ C.Base.ID).asOpt[String]
  ).withCoreAttr(json)
}

case class User(id: Option[String] = None) extends BaseModel {

  override def toJson: JsObject = Json.obj(
    C.Base.ID -> id
  ) ++ coreJsonAttrs

  override def toString: String = s"<user> ${id.getOrElse("NO ID")}"

  def forgetMe(): Unit = {
    println("User: this is where you'd invalidate the saved token in you User model")
  }
}
