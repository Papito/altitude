package altitude.models

import java.io.File

import play.api.libs.json.{JsObject, Json}

class FileImportAsset(val file: File) extends BaseModel {
  require(file != null)

  val absolutePath = file.getAbsolutePath
  val name = file.getName

  override def toString = this.absolutePath
  override def toJson: JsObject = Json.obj(
    "absolutePath" -> absolutePath,
    "name" -> name
  )
}