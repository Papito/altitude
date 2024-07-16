package software.altitude.core.models

import play.api.libs.json._
import software.altitude.core.{Const => C}

import scala.language.implicitConversions

object Face {
  implicit def fromJson(json: JsValue): Face = {

    Face(
      id = (json \ C.Base.ID).asOpt[String],
      x1 = (json \ C.Face.X1).as[Int],
      y1 = (json \ C.Face.Y1).as[Int],
      x2 = (json \ C.Face.X2).as[Int],
      y2 = (json \ C.Face.Y2).as[Int]
    ).withCoreAttr(json)
  }
}

case class Face(id: Option[String] = None, x1: Int, y1: Int, x2: Int, y2: Int) extends BaseModel {

  override def toJson: JsObject = {
    Json.obj(
      C.Face.X1 -> x1,
      C.Face.Y1 -> y1,
      C.Face.X2 -> x2,
      C.Face.Y2 -> y2
    ) ++ coreJsonAttrs
  }

  override def canEqual(other: Any): Boolean = other.isInstanceOf[Face]

  override def equals(that: Any): Boolean = that match {
    case that: Face if !that.canEqual( this) => false
    case that: Face => that.id == this.id
    case _ => false
  }

  override def hashCode: Int = super.hashCode
}
