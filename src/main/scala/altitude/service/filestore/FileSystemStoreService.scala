package altitude.service.filestore

import java.io.{ByteArrayInputStream, InputStream, IOException, File}
import java.nio.file.{Paths, Path}

import altitude.exceptions.{NotFoundException, StorageException}
import altitude.transactions.TransactionId
import altitude.{Const => C, Altitude, Context}
import altitude.models.{Data, Asset, Folder}
import org.apache.commons.io.{FilenameUtils, FileUtils}
import org.slf4j.LoggerFactory

class FileSystemStoreService(app: Altitude) extends FileStoreService {
  private final val log = LoggerFactory.getLogger(getClass)

  override def sortedFolderPath(implicit ctx: Context): String = FilenameUtils.concat("/", C.Path.SORTED)
  override def triageFolderPath(implicit ctx: Context): String = FilenameUtils.concat("/", C.Path.TRIAGE)

  override def addFolder(folder: Folder)(implicit ctx: Context): Unit = {
    val destFile = absoluteFile(folder.path)
    log.debug(s"Adding FS folder [$destFile]")

    try {
      FileUtils.forceMkdir(destFile)
    }
    catch {
      case ex: IOException =>
        throw new StorageException(s"Directory [$destFile] could not be created: $ex")
    }

    if (!(destFile.exists && destFile.isDirectory)) {
      throw new StorageException(s"Directory [$destFile] could not be created")
    }
  }

  override def deleteFolder(folder: Folder)(implicit ctx: Context): Unit = {
    val destFile = absoluteFile(folder.path)
    log.debug(s"Removing FS folder [$destFile]")

    // ignore if not here anymore
    if (!destFile.exists) {
      log.warn(s"Folder [$destFile] no longer exists. Not trying to delete")
      return
    }

    try {
      FileUtils.deleteDirectory(destFile)
    }
    catch {
      case ex: IOException =>
        throw new StorageException(s"Directory [$destFile] could not be deleted: $ex")
    }

    if (destFile.exists || destFile.isDirectory) {
      throw new StorageException(s"Directory [$destFile] could not be deleted")
    }
  }

  override def moveFolder(folder: Folder, newName: String)(implicit ctx: Context): Unit = {
    val srcFile = absoluteFile(folder.path)
    val newPath = FilenameUtils.concat(srcFile.getParent, newName)
    val destFile = absoluteFile(newPath)
    log.debug(s"Moving folder [$srcFile] to [$destFile]")

    if (destFile.exists) {
      throw new StorageException(
        s"Cannot move [$srcFile] to [$destFile]: destination already exists")
    }

    try {
      FileUtils.moveDirectory(srcFile, destFile)
    }
    catch {
      case ex: IOException =>
        throw new StorageException(s"Directory [$srcFile] could not be moved: $ex")
    }

    if (!(destFile.exists && destFile.isDirectory)) {
      throw new StorageException(s"Directory [$srcFile] could not be moved")
    }
  }

  override def getById(id: String)
             (implicit ctx: Context, txId: TransactionId = new TransactionId): Data = {
    val asset: Asset = app.service.library.getById(id)
    val srcFile: File = absoluteFile(asset.path)

    var byteArray: Option[Array[Byte]] = None

    try {
      byteArray = Some(FileUtils.readFileToByteArray(srcFile))
    }
    catch {
      case ex: IOException =>
        throw new StorageException(s"Error reading file [${srcFile.getPath}: $ex]")
    }

    Data(
      assetId = id,
      data = byteArray.get,
      mimeType = "application/octet-stream")
  }

  override def addAsset(asset: Asset)(implicit ctx: Context): Unit = {
    val destFile = absoluteFile(asset.path)
    log.debug(s"Creating asset [$asset] at [$destFile]")
    FileUtils.writeByteArrayToFile(destFile, asset.data)
  }

  override def deleteAsset(asset: Asset)(implicit ctx: Context): Unit = {

  }

  override def moveAsset(asset: Asset, folder: Folder)(implicit ctx: Context): Unit = {

  }

  override def calculateAssetPath(asset: Asset)
                                 (implicit ctx: Context, txId: TransactionId = new TransactionId): String = {
    val folder: Folder = app.service.folder.getById(asset.folderId)
    findNextAvailableFilename(new File(folder.path, asset.fileName))
  }

  override def calculateFolderPath(name: String, parentId: String)
                                  (implicit ctx: Context, txId: TransactionId = new TransactionId): String = {
    val parent: Folder = app.service.folder.getById(parentId)
    new File(parent.path, name).getPath
  }

  /**
   * Get the absolute path to the asset on file system,
   * given path relative to repository root
   */
  private def absoluteFile(relativePath: String)(implicit ctx: Context): File =  {
    val repositoryRoot = ctx.repo.fileStoreConfig(C.Repository.Config.PATH)
    new File(repositoryRoot, relativePath)
  }

  private def findNextAvailableFilename(file: File)(implicit ctx: Context): String = {
    val path = FilenameUtils.getPath(file.getPath)
    val ext = FilenameUtils.getExtension(file.getPath)
    var baseName = FilenameUtils.getBaseName(file.getPath)

    var fileToCheck = new File(path, filenameFromBaseAndExt(baseName, ext))

    var idx = 0
    while (absoluteFile(fileToCheck.getPath).exists) {
      idx += 1
      baseName = baseName + idx
      fileToCheck = new File(path, filenameFromBaseAndExt(baseName, ext))
    }

    fileToCheck.getPath
  }

  private def filenameFromBaseAndExt(baseName: String, ext: String): String = {
    ext.isEmpty match {
      case false => baseName + "." + ext
      case true => baseName
    }
  }
}
