package altitude.service.filestore

import java.io._

import altitude.transactions.TransactionId
import altitude.{Const => C, StorageException, NotFoundException, Altitude, Context}
import altitude.models.{Preview, Data, Asset, Folder}
import org.apache.commons.io.{FilenameUtils, FileUtils}
import org.slf4j.LoggerFactory

class FileSystemStoreService(app: Altitude) extends FileStoreService {
  private final val log = LoggerFactory.getLogger(getClass)

  final override val pathSeparator = File.separator
  final override def sortedFolderPath(implicit ctx: Context): String = C.Path.SORTED
  final override def triageFolderPath(implicit ctx: Context): String = C.Path.TRIAGE
  final override def trashFolderPath(implicit ctx: Context): String = C.Path.TRASH
  final override def landfillFolderPath(implicit ctx: Context): String = C.Path.LANDFILL

  override def addFolder(folder: Folder)(implicit ctx: Context): Unit = {
    require(folder.path.isDefined)
    require(folder.path.get.nonEmpty)

    val destFile = absoluteFile(folder.path.get)
    log.info(s"Adding FS folder [$destFile]")

    try {
      FileUtils.forceMkdir(destFile)
    }
    catch {
      case ex: IOException =>
        throw StorageException(s"Directory [$destFile] could not be created: $ex")
    }

    if (!(destFile.exists && destFile.isDirectory)) {
      throw StorageException(s"Directory [$destFile] could not be created")
    }
  }

  override def deleteFolder(folder: Folder)(implicit ctx: Context): Unit = {
    require(folder.path.isDefined)
    require(folder.path.get.nonEmpty)

    val destFile = absoluteFile(folder.path.get)
    log.info(s"Removing FS folder [$destFile]")

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
        throw StorageException(s"Directory [$destFile] could not be deleted: $ex")
    }

    if (destFile.exists || destFile.isDirectory) {
      throw StorageException(s"Directory [$destFile] could not be deleted")
    }
  }

  override def moveFolder(folder: Folder, newName: String)(implicit ctx: Context): Unit = {
    require(folder.path.isDefined)
    require(folder.path.get.nonEmpty)

    val srcFile = absoluteFile(folder.path.get)
    val newPath = FilenameUtils.concat(srcFile.getParent, newName)
    val destFile = new File(newPath)
    log.info(s"Moving folder [$srcFile] to [$destFile]")

    if (destFile.exists) {
      throw StorageException(
        s"Cannot move [$srcFile] to [$destFile]: destination already exists")
    }

    try {
      FileUtils.moveDirectory(srcFile, destFile)
    }
    catch {
      case ex: IOException =>
        throw StorageException(s"Directory [$srcFile] could not be moved: $ex")
    }

    if (!(destFile.exists && destFile.isDirectory)) {
      throw StorageException(s"Directory [$srcFile] could not be moved")
    }
  }

  override def getById(id: String)
             (implicit ctx: Context, txId: TransactionId = new TransactionId): Data = {
    val asset: Asset = app.service.library.getById(id)
    val path = getAssetPath(asset)
    val srcFile: File = absoluteFile(path)

    var byteArray: Option[Array[Byte]] = None

    try {
      byteArray = Some(FileUtils.readFileToByteArray(srcFile))
    }
    catch {
      case ex: IOException =>
        throw StorageException(s"Error reading file [${srcFile.getPath}: $ex]")
    }

    Data(
      assetId = id,
      data = byteArray.get,
      mimeType = "application/octet-stream")
  }

  override def addAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
    val path = getAssetPath(asset)
    val destFile = absoluteFile(path)
    log.debug(s"Creating asset [$asset] on file system at [$destFile]")

    try {
      FileUtils.writeByteArrayToFile(destFile, asset.data)
    }
    catch {
      case ex: IOException =>
        throw StorageException(s"Error creating [$asset] @ [$destFile]: $ex]")
    }
  }

  override def moveAsset(srcAsset: Asset, destAsset: Asset)
                        (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
    val srcFile = absoluteFile(getAssetPath(srcAsset))
    val destFile = absoluteFile(getAssetPath(destAsset))

    log.debug(s"Moving asset [$srcAsset] on file system from [$srcFile] to [$destFile]")
    moveFile(srcFile, destFile)
  }

  override def recycleAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    log.info(s"Recycling: [$asset]")

    val srcFile = absoluteFile(getAssetPath(asset))
    val relRecyclePath = getRecycledAssetPath(asset)
    val destFile = absoluteFile(relRecyclePath)

    moveFile(srcFile, destFile)
  }

  override def restoreAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    log.info(s"Restoring: [$asset]")

    val relRecyclePath = getRecycledAssetPath(asset)
    val srcFile = absoluteFile(relRecyclePath)
    val destFile = absoluteFile(getAssetPath(asset))

    moveFile(srcFile, destFile)
  }

  override def purgeAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {

  }

  override def getFolderPath(name: String, parentId: String)
                                  (implicit ctx: Context, txId: TransactionId = new TransactionId): String = {
    val parent: Folder = app.service.folder.getById(parentId)
    new File(parent.path.get, name).getPath
  }

  override def calculateNextAvailableFilename(asset: Asset)
                                             (implicit ctx: Context, txId: TransactionId): String = {
    findNextAvailableFilename(new File(getAssetPath(asset)))
  }

  def getPathWithNewFilename(asset: Asset, newFilename: String)
                                  (implicit ctx: Context, txId: TransactionId): String = {
    val folder: Folder = app.service.folder.getById(asset.folderId)
    FilenameUtils.concat(folder.path.get, newFilename)
  }

  override def getAssetPath(asset: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId): String = {
    asset.isRecycled match {
      case false => {
        val folder: Folder = app.service.folder.getById(asset.folderId)
        FilenameUtils.concat(folder.path.get, asset.fileName)
      }
      case true => getRecycledAssetPath(asset)
    }
  }

  override def getRecycledAssetPath(asset: Asset)(implicit ctx: Context): String = {
    val ext = FilenameUtils.getExtension(asset.fileName)
    new File(
      trashFolderPath,
      s"${asset.id.get}${FilenameUtils.EXTENSION_SEPARATOR}$ext").toString
  }

  override def addPreview(preview: Preview)(implicit ctx: Context): Unit = {
    log.info(s"Adding preview for asset ${preview.assetId}")

    // get the full path to our preview file
    val destFilePath = previewFilePath(preview.assetId)
    // parse out the dir path
    val dirPath = FilenameUtils.getFullPath(destFilePath)

    FileUtils.forceMkdir(new File(dirPath))

    try {
      FileUtils.writeByteArrayToFile(new File(destFilePath), preview.data)
    }
    catch {
      case ex: IOException => log.error(s"Could not save preview data to [$destFilePath]")
    }
  }

  override def getPreviewById(assetId: String)(implicit ctx: Context): Preview = {
    val f: File = new File(previewFilePath(assetId))

    if (!f.isFile) {
      throw NotFoundException(s"Cannot find preview for asset '$assetId'")
    }

    val byteArray = FileUtils.readFileToByteArray(f)
    val is: InputStream = new ByteArrayInputStream(byteArray)

    try {
      Preview(
        assetId = assetId,
        data = byteArray,
        mimeType = "application/octet-stream")
    }
    finally {
      if (is != null) is.close()
    }
  }

  override def assemblePath(pathComponents: List[String]): String = {
    pathComponents.mkString(pathSeparator)
  }

  private def previewFilePath(assetId: String)(implicit ctx: Context): String = {
    val dirName = assetId.substring(0, 2)
    new File(new File(previewDirPath, dirName).getPath, assetId + ".png").getPath
  }

  private def previewDirPath(implicit ctx: Context): String =
    new File(ctx.repo.fileStoreConfig(C.Repository.Config.PATH), "p").getPath

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
      baseName = s"${baseName}_$idx"
      fileToCheck = new File(path, filenameFromBaseAndExt(baseName, ext))
    }

    fileToCheck.getName
  }

  private def moveFile(srcFile: File, destFile: File): Unit = {
    log.info(s"Moving [$srcFile] to [$destFile]")

    try {
      if (destFile.exists) {
        throw StorageException(s"Error moving [$srcFile] to [$destFile]: destination exists]")
      }

      FileUtils.moveFile(srcFile, destFile)
    }
    catch {
      case ex: IOException =>
        throw StorageException(s"Error moving [$srcFile] to [$destFile]: $ex]")
    }
  }

  private def filenameFromBaseAndExt(baseName: String, ext: String): String = {
    ext.isEmpty match {
      case false => baseName + "." + ext
      case true => baseName
    }
  }
}
