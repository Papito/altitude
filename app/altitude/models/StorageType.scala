package altitude.models

import play.api.libs.json.JsValue

import scala.language.implicitConversions

object StorageType extends Enumeration {
  type StorageType = Value
  val file_system, amazon_s3, amazon_cloud_drvie, google_drive = Value

  implicit def fromJson(jsVal: JsValue): StorageType = StorageType.Value(jsVal.toString())
}