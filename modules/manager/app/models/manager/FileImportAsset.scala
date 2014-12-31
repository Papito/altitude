package models.manager

import java.io.File

import models.BaseModel

class FileImportAsset(val file: File) extends BaseModel {
  require(file != null)

  val absolutePath = file.getAbsolutePath
  val name = file.getName

  override def toString = this.absolutePath
}