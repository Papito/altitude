package dao.manager

import models.ImportAsset

class ImportDao {

  def getImportAssets(): List[ImportAsset] = {
    val assets: List[ImportAsset] = List[ImportAsset](new ImportAsset("file://path"))
    return assets
  }
}
