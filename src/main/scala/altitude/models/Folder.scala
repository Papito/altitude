package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{JsNull, JsValue, Json}

import scala.language.implicitConversions

object Folder {
  implicit def fromJson(json: JsValue): Folder = Folder(
    id = (json \ C.Folder.ID).asOpt[String],
    name = (json \ C.Folder.NAME).as[String],
    parentId = (json \ C.Folder.PARENT_ID).asOpt[String],
    size = (json \ C.Folder.SIZE).as[Int]
  ).withCoreAttr(json)
}

case class Folder(id: Option[String] = None,
                  parentId: Option[String] = None,
                  name: String,
                  size: Int = 0) extends BaseModel {

  override def toJson = Json.obj(
    C.Folder.NAME -> name,
    C.Folder.PARENT_ID -> {parentId match {
      case None => JsNull
      case _ => parentId.get
    }},
    C.Folder.SIZE -> size
  ) ++ coreJsonAttrs
}
