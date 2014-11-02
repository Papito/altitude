package dao.manager

import play.api.Play
import models.ImportAsset
import util.log

class ImportDao {

  def getImportAssets: List[ImportAsset] = {

    val importPath = Play.current.configuration.getString("import.path").toString
    log.info("Importing from '$importPath'", Map("importPath" -> importPath), log.STORAGE)
    List[ImportAsset](new ImportAsset("file://path"))
  }
}
