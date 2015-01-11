package altitude.models

import java.io.File
import play.api.libs.json.{JsObject, Json}
import reactivemongo.bson.BSONObjectID

class FileImportAsset(val file: File) extends BaseModel[String] {
  require(file != null)

  val absolutePath = file.getAbsolutePath
  val name = file.getName

  override def toString = this.absolutePath
  override def toJson: JsObject = Json.obj(
    "absolutePath" -> absolutePath,
    "name" -> name
  )
  override protected def genId: String = BSONObjectID.generate.toString()
}