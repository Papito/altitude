package service.manager

import dao.manager.ImportDao
import models.ImportAsset
import util.log
import constants.{const => C}

class ImportService {

  private val DAO = new ImportDao

  def getImportAssets(path: String): List[ImportAsset] = {
    require(path.nonEmpty)
    log.info("In import service to import from $path", Map("path" -> path), C.tag.SERVICE)

    this.DAO.getImportAssets(path = path)
  }
}
