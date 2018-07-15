package software.altitude.core.controllers

import play.api.libs.json.{JsObject, Json}
import software.altitude.core.models.{Asset, MetadataField}
import software.altitude.core.{Altitude, Context, Const => C}

object Util {
  def withFormattedMetadata(app: Altitude, asset: Asset, allFields: Option[Map[String, MetadataField]] = None)
                    (implicit ctx: Context): JsObject = {
    val metadata = app.service.metadata.toJson(asset.metadata, allFields)
    asset.toJson ++ Json.obj(C.Asset.METADATA -> metadata)
  }
}
