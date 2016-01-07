package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{JsArray, JsNull, JsValue, Json}

import scala.language.implicitConversions

object Folder {
  implicit def fromJson(json: JsValue): Folder = {
    val childrenJson = (json \ C.Folder.CHILDREN).as[List[JsValue]];

    Folder(
      id = (json \ C.Folder.ID).asOpt[String],
      name = (json \ C.Folder.NAME).as[String],
      children = childrenJson.map(Folder.fromJson),
      parentId = (json \ C.Folder.PARENT_ID).as[String],
      numOfAssets = (json \ C.Folder.NUM_OF_ASSETS).as[Int]
    ).withCoreAttr(json)
  }

  val UNCATEGORIZED = Folder(
    id = Some(C.Folder.Ids.UNCATEGORIZED),
    name = C.Folder.Names.UNCATEGORIZED
  )

  val ROOT = Folder(
    id = Some(C.Folder.Ids.ROOT),
    name = C.Folder.Names.ROOT
  )

  val SYSTEM_FOLDERS: List[Folder] = List(UNCATEGORIZED)

  def IS_ROOT(id:  Option[String]) = id == ROOT.id
  def IS_UNCATEGORIZED(id: Option[String]) = id == UNCATEGORIZED.id
  def IS_SYSTEM(id:  Option[String]) = SYSTEM_FOLDERS.exists(_.id == id)
}

case class Folder(id: Option[String] = None,
                  parentId: String = C.Folder.Ids.ROOT,
                  name: String,
                  children: List[Folder] = List(),
                  numOfAssets: Int = 0) extends BaseModel {

  val nameLowercase = name.toLowerCase

  def isRoot = Folder.IS_ROOT(id)
  def isUncategorized = Folder.IS_UNCATEGORIZED(id)
  def isSystem = Folder.IS_SYSTEM(id)

  override def toJson = {
    val childrenJson: List[JsValue] = children.map(_.toJson)
    Json.obj(
      C.Folder.NAME -> name,
      C.Folder.NAME_LC -> nameLowercase,
      C.Folder.PARENT_ID -> parentId,
      C.Folder.CHILDREN ->  JsArray(childrenJson),
      C.Folder.NUM_OF_ASSETS -> numOfAssets
    ) ++ coreJsonAttrs
  }
}
