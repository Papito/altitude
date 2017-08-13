package altitude.controllers

import altitude.{Context, Altitude, Const => C}
import altitude.models.{MetadataField, Asset}
import play.api.libs.json.{Json, JsObject}

object Util {
  def withFormattedMetadata(app: Altitude, asset: Asset, allFields: Option[Map[String, MetadataField]] = None)
                    (implicit ctx: Context): JsObject = {
    val metadata = app.service.metadata.toJson(asset.metadata, allFields)
    asset.toJson ++ Json.obj(C.Asset.METADATA -> metadata)
  }
}
