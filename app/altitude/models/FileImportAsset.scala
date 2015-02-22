package altitude.models

import java.io.File

import play.api.libs.json.{JsObject, Json}
import scala.language.implicitConversions

object FileImportAsset {
  implicit def toJson(obj: FileImportAsset) = Json.obj(
    "absolutePath" -> obj.absolutePath,
    "name" -> obj.name
  )
}

class FileImportAsset(val file: File) extends BaseModel {
  require(file != null)

  val absolutePath = file.getAbsolutePath
  val name = file.getName

  override def toString = this.absolutePath
}