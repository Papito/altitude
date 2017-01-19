package altitude.models

import play.api.libs.json.JsValue

import scala.language.implicitConversions

object StorageType extends Enumeration {
  val FS, S3 = Value

  implicit def fromJson(jsVal: JsValue): StorageType.Value = StorageType.Value(jsVal.toString())
}