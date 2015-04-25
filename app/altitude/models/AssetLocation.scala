package altitude.models

import altitude.{Const => C}
import play.api.libs.json.{JsObject, Json, JsValue}
import scala.language.implicitConversions

object AssetLocation {
  implicit def fromJson(json: JsValue): AssetLocation = AssetLocation(
    locId = (json \ C.AssetLocation.STORAGE_ID).as[Int],
    path = (json \ C.AssetLocation.PATH).as[String]
  )
}

case class AssetLocation(locId: Int, path: String) extends BaseModel with NoId {
  override def toJson = Json.obj(
    C.AssetLocation.STORAGE_ID -> locId,
    C.AssetLocation.PATH -> path)
}


