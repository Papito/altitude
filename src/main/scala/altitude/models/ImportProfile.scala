package altitude.models

import altitude.{Const => C}
import play.api.libs.json._

import scala.language.implicitConversions

/*
object ImportProfile {
  implicit def fromJson(json: JsValue): ImportProfile = ImportProfile(
      id = (json \ C.ImportProfile.ID).asOpt[String],
      name = (json \ C.ImportProfile.NAME).as[String],
      tagData = json \ C.ImportProfile.TAG_DATA
    ).withCoreAttr(json)
}

case class ImportProfile(id: Option[String] = None,
                         name: String,
                         tagData: JsValue) extends BaseModel {
  override def toJson = Json.obj(
    C.ImportProfile.NAME -> name,
    C.ImportProfile.TAG_DATA -> tagData
  ) ++ coreJsonAttrs
}
*/
