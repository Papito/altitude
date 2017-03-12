package altitude.service.filestore

import java.io._

import altitude.transactions.TransactionId
import altitude.{Const => C, StorageException, NotFoundException, Altitude, Context}
import altitude.models.{Preview, Data, Asset, Folder}
import org.apache.commons.io.{FilenameUtils, FileUtils}
import org.slf4j.LoggerFactory

class FileSystemStoreService(app: Altitude) extends FileStoreService {
  private final val log = LoggerFactory.getLogger(getClass)

  override def sortedFolderPath(implicit ctx: Context): String = C.Path.SORTED
  override def triageFolderPath(implicit ctx: Context): String = C.Path.TRIAGE
  override def trashFolderPath(implicit ctx: Context): String = C.Path.TRASH
  override def landfillFolderPath(implicit ctx: Context): String = C.Path.LANDFILL

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
    log.debug(s"Creating asset [$asset] on file system at [$destFile]")

    try {
      FileUtils.writeByteArrayToFile(destFile, asset.data)
    }
    catch {
      case ex: IOException =>
        throw new StorageException(s"Error creating [$asset] @ [$destFile]: $ex]")
    }
  }

  override def moveAsset(asset: Asset, destPath: String)(implicit ctx: Context): Unit = {
    val srcFile = absoluteFile(getAssetPath(asset))
    val destFile = absoluteFile(destPath)

    log.debug(s"Moving asset [$asset] on file system from [$srcFile] to [$destFile]")
    moveFile(srcFile, destFile)
  }

  override def recycleAsset(asset: Asset)(implicit ctx: Context) = {
    log.info(s"Recycling: [$asset]")

    val srcFile = absoluteFile(asset.path)
    val relRecyclePath = getRecycledAssetPath(asset)
    val destFile = absoluteFile(relRecyclePath)

    moveFile(srcFile, destFile)
  }

  override def restoreAsset(asset: Asset)(implicit ctx: Context) = {
    log.info(s"Restoring: [$asset]")

    val srcFile = absoluteFile(getAssetPath(asset))
    val destFile = absoluteFile(asset.path)

    moveFile(srcFile, destFile)
  }

  override def purgeAsset(asset: Asset)(implicit ctx: Context): Unit = {

  }

  override def calculateFolderPath(name: String, parentId: String)
                                  (implicit ctx: Context, txId: TransactionId = new TransactionId): String = {
    val parent: Folder = app.service.folder.getById(parentId)
    new File(parent.path, name).getPath
  }

  override def calculateAssetPath(asset: Asset, folder: Folder)
                        (implicit ctx: Context, txId: TransactionId = new TransactionId): String = {

        findNextAvailableFilename(new File(folder.path, asset.fileName))
  }

  override def getAssetPath(asset: Asset)(implicit ctx: Context): String = {
    asset.isRecycled match {
      case false => asset.path
      case true => getRecycledAssetPath(asset)
    }
  }

  override def getRecycledAssetPath(asset: Asset)(implicit ctx: Context): String = {
    val ext = FilenameUtils.getExtension(asset.path)
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

    fileToCheck.getPath
  }

  private def moveFile(src: File, dest: File): Unit = {
    log.debug(s"Moving [$src] to [$dest]")

    try {
      FileUtils.moveFile(src, dest)
    }
    catch {
      case ex: IOException =>
        throw new StorageException(s"Error moving [$dest] to [$dest]: $ex]")
    }
  }

  private def filenameFromBaseAndExt(baseName: String, ext: String): String = {
    ext.isEmpty match {
      case false => baseName + "." + ext
      case true => baseName
    }
  }
}
