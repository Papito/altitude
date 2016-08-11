package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.language.implicitConversions

object Folder {
  implicit def fromJson(json: JsValue): Folder = {
    val childrenJson = (json \ C("Folder.CHILDREN")).as[List[JsValue]]

    Folder(
      id = (json \ C("Base.ID")).asOpt[String],
      userId = (json \ C("Base.USER_ID")).as[String],
      name = (json \ C("Folder.NAME")).as[String],
      children = childrenJson.map(Folder.fromJson),
      parentId = (json \ C("Folder.PARENT_ID")).as[String],
      numOfAssets = (json \ C("Folder.NUM_OF_ASSETS")).as[Int]
    ).withCoreAttr(json)
  }
}

case class Folder(id: Option[String] = None,
                  userId: String,
                  parentId: String,
                  name: String,
                  children: List[Folder] = List(),
                  numOfAssets: Int = 0) extends BaseModel {

  val nameLowercase = name.toLowerCase

  override def toJson = {
    val childrenJson: List[JsValue] = children.map(_.toJson)
    Json.obj(
      C("Base.USER_ID") -> userId,
      C("Folder.NAME") -> name,
      C("Folder.NAME_LC") -> nameLowercase,
      C("Folder.PARENT_ID") -> parentId,
      C("Folder.CHILDREN") ->  JsArray(childrenJson),
      C("Folder.NUM_OF_ASSETS") -> numOfAssets
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
