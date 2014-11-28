package dao.manager

import org.apache.commons.io.FileUtils
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import java.io.File

import models.ImportAsset
import util.log
import constants.{const => C}

class ImportDao {
  val imageSuffixes = List("jpg", "jpeg")

  def getImportAssets(path: String): List[ImportAsset] = {
    log.info("Importing from 'path'", Map("importPath" -> path), C.tag.STORAGE)

    val fileIterator = FileUtils.iterateFiles(new File(path), imageSuffixes.toArray, true)
    val assets = new ListBuffer[ImportAsset]

    for(file <- fileIterator) {
      val f = new File(file.toString)
      log.info("Processing $fileName", Map("fileName" -> file.toString), C.tag.STORAGE)

      val importAsset = new ImportAsset(
        pathArg = f.getAbsolutePath
      )
      assets += importAsset
    }

    assets.toList
  }

}
