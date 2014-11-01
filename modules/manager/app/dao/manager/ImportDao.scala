package dao.manager

import models.ImportAsset

class ImportDao {

  def getImportAssets: List[ImportAsset] = {
    List[ImportAsset](new ImportAsset("file://path"))
  }
}
