package altitude.models

import altitude.{Const => C}
import play.api.libs.json._

import scala.language.implicitConversions

object ImportProfile {
  implicit def fromJson(json: JsValue): ImportProfile = ImportProfile(
    id = (json \ C.ImportProfile.ID).asOpt[String],
    name = (json \ C.ImportProfile.NAME).as[String]
  ).withCoreAttr(json)
}

case class ImportProfile(id: Option[String] = None,
                         name: String) extends BaseModel {

  override val toJson = Json.obj(
    C.ImportProfile.NAME -> name
  ) ++ coreJsonAttrs
}
