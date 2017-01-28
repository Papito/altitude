package altitude.models

import play.api.libs.json.Json

class ImportAsset(val path: String,
                  val data: Array[Byte],
                  val sourceType: StorageType.Value,
                  val metadata: Metadata)
  extends BaseModel with NoId {

  override val toJson = Json.obj(
    "path" -> path,
    "sourceType" -> sourceType.toString
  )
}