package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.language.implicitConversions

object Folder {
  implicit def fromJson(json: JsValue): Folder = {
    val childrenJson = (json \ C("Folder.CHILDREN")).as[List[JsValue]]

    Folder(
      id = (json \ C("Base.ID")).asOpt[String],
      name = (json \ C("Folder.NAME")).as[String],
      children = childrenJson.map(Folder.fromJson),
      parentId = (json \ C("Folder.PARENT_ID")).as[String],
      numOfAssets = (json \ C("Folder.NUM_OF_ASSETS")).as[Int]
    ).withCoreAttr(json)
  }

  val ROOT = Folder(
    id = Some(C("Folder.Ids.ROOT")),
    name = C("Folder.Names.ROOT")
  )

  val UNCATEGORIZED = Folder(
    id = Some(C("Folder.Ids.UNCATEGORIZED")),
    name = C("Folder.Names.UNCATEGORIZED")
  )

  val TRASH = Folder(
    id = Some(C("Folder.Ids.TRASH")),
    name = C("Folder.Names.TRASH")
  )

  val SYSTEM_FOLDERS: List[Folder] = List(UNCATEGORIZED, TRASH)

  def IS_ROOT(id:  Option[String]) = id == ROOT.id
  def IS_SYSTEM(id:  Option[String]) = SYSTEM_FOLDERS.exists(_.id == id)
}

case class Folder(id: Option[String] = None,
                  parentId: String = C("Folder.Ids.ROOT"),
                  name: String,
                  children: List[Folder] = List(),
                  numOfAssets: Int = 0) extends BaseModel {

  val nameLowercase = name.toLowerCase

  override def toJson = {
    val childrenJson: List[JsValue] = children.map(_.toJson)
    Json.obj(
      C("Folder.NAME") -> name,
      C("Folder.NAME_LC") -> nameLowercase,
      C("Folder.PARENT_ID") -> parentId,
      C("Folder.CHILDREN") ->  JsArray(childrenJson),
      C("Folder.NUM_OF_ASSETS") -> numOfAssets
    ) ++ coreJsonAttrs
  }

  def canEqual(other: Any) = other.isInstanceOf[Folder]

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
