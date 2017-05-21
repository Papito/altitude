package altitude.models

import play.api.libs.json.Json
import altitude.{Const => C}

class ImportAsset(val path: String,
                  val fileName: String,
                  val data: Array[Byte],
                  val sourceType: C.FileStoreType.Value,
                  val metadata: Metadata)
  extends BaseModel with NoId {

  override val toJson = Json.obj(
    "fileName" -> fileName,
    "path" -> path,
    "sourceType" -> sourceType.toString
  )
}