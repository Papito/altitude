package models

import java.io.File

class ImportAsset(val file: File) extends BaseModel {
  require(file != null)

  val absolutePath = file.getAbsolutePath
  val name = file.getName
  var mediaType: AssetMediaType = null

  override def toDict = Map(
    "path" -> absolutePath
  )

  override def toString = this.absolutePath
}