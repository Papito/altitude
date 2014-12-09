package service.manager

import dao.manager.ImportDao
import models.ImportAsset
import util.log
import constants.{const => C}

class ImportService {

  private val DAO = new ImportDao

  def getImportAssets(path: String): List[ImportAsset] = {
    require(path.nonEmpty)
    log.info("Getting assets to import in '$path'", Map("path" -> path), C.tag.SERVICE)

    this.DAO.getImportAssets(path = path)
  }

  def importAssets(path: String): Unit = {
    require(path.nonEmpty)
    log.info("Importing assets in '$path'", Map("path" -> path), C.tag.SERVICE)

    val importAssets = getImportAssets(path)

    for (assetToImport <- importAssets) {
      importAsset(assetToImport)
    }
  }

  def importAsset(importAsset: ImportAsset): Unit = {
    log.info("Importing asset: $asset", Map("asset" -> importAsset), C.tag.SERVICE)

  }
}
