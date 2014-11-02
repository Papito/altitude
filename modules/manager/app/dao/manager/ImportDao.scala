package dao.manager

import play.api.Play
import org.apache.commons.io.FileUtils
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import java.io.File

import models.ImportAsset
import util.log

class ImportDao {
  val imageSuffixes = List("jpg", "jpeg")

  def getImportAssets: List[ImportAsset] = {
    val importPath = Play.current.configuration.getString("import.path").getOrElse("")
    log.info("Importing from '$importPath'", Map("importPath" -> importPath), log.STORAGE)

    val fileIterator = FileUtils.iterateFiles(new File(importPath), imageSuffixes.toArray, true)
    val assets = new ListBuffer[ImportAsset]

    for(file <- fileIterator) {
      val f = new File(file.toString)
      log.info("Processing $fileName", Map("fileName" -> file.toString), log.STORAGE)

      val importAsset = new ImportAsset(
        pathArg = f.getAbsolutePath
      )
      assets += importAsset
    }

    assets.toList
  }

}
