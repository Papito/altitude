package altitude.service.filestore

import java.io.{IOException, File}

import altitude.exceptions.StorageException
import altitude.transactions.TransactionId
import altitude.{Const => C, Altitude, Context}
import altitude.models.{Asset, Folder}
import org.apache.commons.io.{FilenameUtils, FileUtils}
import org.slf4j.LoggerFactory

class FileSystemStoreService(app: Altitude) extends FileStoreService {
  private final val log = LoggerFactory.getLogger(getClass)

  private def fullPath(relativePath: String)(implicit ctx: Context): File =  {
    val rootDirectory = ctx.repo.fileStoreConfig(C.Repository.Config.PATH)
    new File(rootDirectory, relativePath)
  }

  override def addFolder(folder: Folder)(implicit ctx: Context): Unit = {
    val folderPath = fullPath(folder.path)
    log.debug(s"Adding FS folder [$folderPath]")

    try {
      FileUtils.forceMkdir(folderPath)
    }
    catch {
      case ex: IOException =>
        throw new StorageException(s"Directory [$folderPath] could not be created: $ex")
    }

    if (!(folderPath.exists && folderPath.isDirectory)) {
      throw new StorageException(s"Directory [$folderPath] could not be created")
    }
  }

  override def deleteFolder(folder: Folder)(implicit ctx: Context): Unit = {
    val folderPath = fullPath(folder.path)
    log.debug(s"Removing FS folder [$folderPath]")

    // ignore if not here anymore
    if (!folderPath.exists) {
      log.warn(s"Folder [$folderPath] no longer exists. Not trying to delete")
      return
    }

    try {
      FileUtils.deleteDirectory(folderPath)
    }
    catch {
      case ex: IOException =>
        throw new StorageException(s"Directory [$folderPath] could not be deleted: $ex")
    }

    if (folderPath.exists || folderPath.isDirectory) {
      throw new StorageException(s"Directory [$folderPath] could not be deleted")
    }
  }

  override def moveFolder(folder: Folder, newName: String)(implicit ctx: Context): Unit = {
    val srcFolderPath = fullPath(folder.path)
    val newPath = FilenameUtils.concat(srcFolderPath.getParent, newName)
    val destFolderPath = fullPath(newPath)
    log.debug(s"Moving folder [$srcFolderPath] to [$destFolderPath]")

    if (destFolderPath.exists) {
      throw new StorageException(
        s"Cannot move [$srcFolderPath] to [$destFolderPath]: destination already exists")
    }

    try {
      FileUtils.moveDirectory(srcFolderPath, destFolderPath)
    }
    catch {
      case ex: IOException =>
        throw new StorageException(s"Directory [$srcFolderPath] could not be moved: $ex")
    }

    if (!(destFolderPath.exists && destFolderPath.isDirectory)) {
      throw new StorageException(s"Directory [$srcFolderPath] could not be moved")
    }
  }

  override def addAsset(asset: Asset)(implicit ctx: Context): Unit = {
    log.debug(s"Creating asset [$asset] at [${asset.path}]")
  }

  override def deleteAsset(asset: Asset)(implicit ctx: Context): Unit = {

  }

  override def moveAsset(asset: Asset, folder: Folder)(implicit ctx: Context): Unit = {

  }

  override def calculateAssetPath(asset: Asset)
                                 (implicit ctx: Context, txId: TransactionId = new TransactionId): String = {
    ""
  }

  override def calculateFolderPath(name: String, parentId: String)
                                  (implicit ctx: Context, txId: TransactionId = new TransactionId): String = {
    val parent: Folder = app.service.folder.getById(parentId)
    FilenameUtils.concat(parent.path, name)
  }

  override def sortedFolderPath(implicit ctx: Context): String = FilenameUtils.concat("/", C.Path.SORTED)
  override def triageFolderPath(implicit ctx: Context): String = FilenameUtils.concat("/", C.Path.TRIAGE)

}
