package altitude.models

import java.io.File

import play.api.libs.json.Json

import scala.language.implicitConversions

class FileImportAsset(val file: File) extends BaseModel with NoId {
  require(file != null)

  val absolutePath = file.getAbsolutePath
  val name = file.getName

  override def toJson = Json.obj(
    "absolutePath" -> absolutePath,
    "name" -> name
  )

  override def toString = this.absolutePath
}