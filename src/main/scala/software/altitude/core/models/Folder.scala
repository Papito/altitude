package software.altitude.core.models

import software.altitude.core.{Const => C}
import play.api.libs.json.{JsString, JsArray, JsValue, Json}

import scala.language.implicitConversions

object Folder {
  implicit def fromJson(json: JsValue): Folder = {
    val childrenJson = (json \ C.Folder.CHILDREN).as[List[JsValue]]

    Folder(
      id = (json \ C.Base.ID).asOpt[String],
      name = (json \ C.Folder.NAME).as[String],
      path = (json \ C.Folder.PATH).asOpt[String],
      children = childrenJson.map(Folder.fromJson),
      parentId = (json \ C.Folder.PARENT_ID).as[String],
      numOfAssets = (json \ C.Folder.NUM_OF_ASSETS).as[Int]
    ).withCoreAttr(json)
  }
}

case class Folder(id: Option[String] = None,
                  parentId: String,
                  name: String,
                  path: Option[String] = None,
                  children: List[Folder] = List(),
                  numOfAssets: Int = 0) extends BaseModel {

  val nameLowercase = name.toLowerCase

  override def toJson = {
    val childrenJson: List[JsValue] = children.map(_.toJson)
    Json.obj(
      C.Folder.NAME -> name,
      C.Folder.PATH -> JsString(path.getOrElse("")),
      C.Folder.NAME_LC -> nameLowercase,
      C.Folder.PARENT_ID -> parentId,
      C.Folder.CHILDREN ->  JsArray(childrenJson),
      C.Folder.NUM_OF_ASSETS -> numOfAssets
    ) ++ coreJsonAttrs
  }

  override def canEqual(other: Any): Boolean = other.isInstanceOf[Folder]

  override def equals( that: Any): Boolean = that match {
    case that: Folder if !that.canEqual( this) => false
    case that: Folder => {
      val thisStringRepr = this.id.getOrElse("") + this.parentId + this.name
      val thatStringRepr = that.id.getOrElse("") + that.parentId + that.name
      thisStringRepr == thatStringRepr
    }
    case _ => false
  }
}
