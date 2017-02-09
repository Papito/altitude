package altitude.service.filestore

import java.io.File

import altitude.{Const => C, Altitude, Context}
import altitude.models.{Asset, Folder}
import org.slf4j.LoggerFactory

class FileSystemStoreService(app: Altitude) extends FileStoreService {
  private final val log = LoggerFactory.getLogger(getClass)

  override def addFolder(folder: Folder)(implicit ctx: Context): Unit = {
    val folderPath = fullPath(folder.path)
    log.debug(s"Adding FS folder [$folderPath]")
    FileUtils.forceMkdir(folderPath)
  }

  override def deleteFolder(folder: Folder)(implicit ctx: Context): Unit = {
  }

  override def renameFolder(folder: Folder, newName: String)(implicit ctx: Context): Unit = {
    log.debug(s"Adding folder $folder")
  }

  override def addAsset(asset: Asset)(implicit ctx: Context): Unit = {
    log.debug(s"Adding asset $asset")
  }

  override def deleteAsset(asset: Asset)(implicit ctx: Context): Unit = {

  }

  override def moveAsset(asset: Asset, folder: Folder)(implicit ctx: Context): Unit = {

  }

  private def fullPath(relativePath: String)(implicit ctx: Context): File =  {
    val rootDirectory = ctx.repo.fileStoreConfig(C.Repository.Config.PATH)
    new File(rootDirectory, relativePath)
  }
}
