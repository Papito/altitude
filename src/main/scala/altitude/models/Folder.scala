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
      parentId = (json \ C.Folder.PARENT_ID).asOpt[String],
      numOfAssets = (json \ C.Folder.NUM_OF_ASSETS).as[Int]
    ).withCoreAttr(json)
  }
}

case class Folder(id: Option[String] = None,
                  parentId: Option[String] = None,
                  name: String,
                  children: List[Folder] = List(),
                  numOfAssets: Int = 0) extends BaseModel {

  override def toJson = {
    val childrenJson: List[JsValue] = children.map(_.toJson)
    Json.obj(
      C.Folder.NAME -> name,
      C.Folder.PARENT_ID -> {parentId match {
        case None => JsNull
        case _ => parentId.get
      }},
      C.Folder.CHILDREN ->  JsArray(childrenJson),
      C.Folder.NUM_OF_ASSETS -> numOfAssets
    ) ++ coreJsonAttrs
  }
}
