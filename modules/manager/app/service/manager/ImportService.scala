package service.manager

import dao.manager.ImportDao
import models.ImportAsset
import util.log

class ImportService {

  private val DAO = new ImportDao

  def getImportAssets: List[ImportAsset] = {
    log.info("In import service")
    this.DAO.getImportAssets
  }
}
