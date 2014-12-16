package models

import java.io.File

class ImportAsset(val file: File, val mediaType: AssetMediaType) extends BaseModel {
  require(file != null)

  def this(file: File) = this(file, null)

  val absolutePath = file.getAbsolutePath
  val name = file.getName

  override def toMap = Map(
    "id" -> id,
    "path" -> absolutePath,
    "mediaType" -> (if (mediaType != null) mediaType.toMap else null)
  )

  override def toString = this.absolutePath
}