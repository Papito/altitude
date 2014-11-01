package service.manager

import dao.manager.ImportDao
import models.ImportAsset
import util.log

class ImportService {

  private val DAO: ImportDao = new ImportDao

  def getImportAssets(): List[ImportAsset] = {
    log.info("In import service")
    val assets = this.DAO.getImportAssets()
    return assets
  }
}
