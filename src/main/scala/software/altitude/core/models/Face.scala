package software.altitude.core.models

import play.api.libs.json._
import software.altitude.core.{Const => C}

import scala.language.implicitConversions

object Face {
  implicit def fromJson(json: JsValue): Face = {

    Face(
      id = (json \ C.Base.ID).asOpt[String]
    ).withCoreAttr(json)
  }
}

case class Face(id: Option[String] = None) extends BaseModel {

  override def toJson: JsObject = {
    Json.obj(
    ) ++ coreJsonAttrs
  }

  override def canEqual(other: Any): Boolean = other.isInstanceOf[Face]

  override def equals(that: Any): Boolean = that match {
    case that: Folder if !that.canEqual( this) => false
    case that: Folder => that.id == this.id
    case _ => false
  }

  override def hashCode: Int = super.hashCode
}
