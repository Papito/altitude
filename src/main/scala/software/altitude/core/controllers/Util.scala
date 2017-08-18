package software.altitude.core.controllers

import software.altitude.core.{Context, Altitude, Const => C}
import software.altitude.core.models.{MetadataField, Asset}
import play.api.libs.json.{Json, JsObject}

object Util {
  def withFormattedMetadata(app: Altitude, asset: Asset, allFields: Option[Map[String, MetadataField]] = None)
                    (implicit ctx: Context): JsObject = {
    val metadata = app.service.metadata.toJson(asset.metadata, allFields)
    asset.toJson ++ Json.obj(C.Asset.METADATA -> metadata)
  }
}
