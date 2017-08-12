package altitude.controllers

import altitude.{Context, Altitude}
import altitude.models.{MetadataField, Asset}
import play.api.libs.json.JsObject

object Utils {
  def formatMetadata(app: Altitude, asset: Asset, allFields: Option[Map[String, MetadataField]] = None)
                    (implicit ctx: Context): JsObject = {
    asset.toJson ++ app.service.metadata.toJson(asset.metadata, allFields)
  }

}
