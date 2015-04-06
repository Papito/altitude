package altitude.models

import java.io.File

import play.api.libs.json.Json

import scala.language.implicitConversions

object FileImportAsset {
  implicit def toJson(obj: FileImportAsset) = obj.toJson
}

class FileImportAsset(val file: File) extends BaseModel {
  require(file != null)

  val absolutePath = file.getAbsolutePath
  val name = file.getName

  override def toJson = Json.obj(
    "absolutePath" -> absolutePath,
    "name" -> name
  )

  override def toString = this.absolutePath
}