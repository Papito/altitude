package models

import java.io.File

class ImportAsset(argFile: File) {
  require(argFile != null)

  val absolutePath = file.getAbsolutePath
  val name = file.getName
  var mediaType: AssetMediaType = null

  def file = argFile

  def toDict = Map(
    "path" -> absolutePath
  )

  override def toString = this.absolutePath
}

