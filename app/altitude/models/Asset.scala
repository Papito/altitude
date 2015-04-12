package altitude.models

import play.api.libs.json._
import altitude.{Const => C}

import scala.language.implicitConversions

object Asset {
  implicit def fromJson(json: JsValue): Asset = {
    val locationsJs: JsArray = (json \ C.Asset.LOCATIONS).as[JsArray]

    new Asset(
      id = (json \ C.Asset.ID).as[String],
      mediaType = json \ C.Asset.MEDIA_TYPE,
      locations = locationsJs.value.map(StorageLocation.fromJson).toList,
      metadata = json \ C.Asset.METADATA
    )
  }
}

case class Asset(override final val id: String = BaseModel.genId,
                 mediaType: MediaType,
                 locations: List[StorageLocation],
                 metadata: JsValue = JsNull) extends BaseModel {

  override def toJson = Json.obj(
      C.Asset.ID -> id,
      C.Asset.LOCATIONS -> Json.arr(  ),
      C.Asset.MEDIA_TYPE -> (mediaType: JsValue),
      C.Asset.METADATA -> metadata
    )
}