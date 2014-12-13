package dao.manager

import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{IOFileFilter, TrueFileFilter}
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

import models.ImportAsset
import util.log
import constants.{const => C}

class ImportDao {
  private val ANY_FILE_FILTER: IOFileFilter = TrueFileFilter.INSTANCE

  def getImportAssets(path: String): List[ImportAsset] = {
    log.info("Importing from 'importPath'", Map("importPath" -> path), C.tag.DB)
    require(path != null)

    val files = FileUtils.iterateFiles(new File(path), ANY_FILE_FILTER, ANY_FILE_FILTER)
    val assets = new ListBuffer[ImportAsset]

    for(fileIt <- files) {
      log.info("Processing '$file'", Map("file" -> fileIt), C.tag.DB)
      val file: File = new File(fileIt.toString)
      val importAsset = new ImportAsset(file)
      assets += importAsset
    }

    assets.toList
  }

}
