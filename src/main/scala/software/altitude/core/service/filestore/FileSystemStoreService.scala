package software.altitude.core.service.filestore

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.NotFoundException
import software.altitude.core.RequestContext
import software.altitude.core.StorageException
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.MimedAssetData
import software.altitude.core.models.MimedPreviewData
import software.altitude.core.{Const => C}

import java.io._

class FileSystemStoreService(app: Altitude) extends FileStoreService {
  protected final val logger: Logger = LoggerFactory.getLogger(getClass)

  override def getAssetById(id: String): MimedAssetData = {
    val path = filePath(id)
    val srcFile: File = new File(path)

    var byteArray: Option[Array[Byte]] = None

    try {
      byteArray = Some(FileUtils.readFileToByteArray(srcFile))
    }
    catch {
      case ex: IOException =>
        throw StorageException(s"Error reading file [${srcFile.getPath}: $ex]")
    }

    MimedAssetData(
      assetId = id,
      data = byteArray.get,
      mimeType = "application/octet-stream")
  }

  override def addAsset(asset: Asset): Unit = {
    val destFile = new File(filePath(asset.persistedId))
    logger.info(s"Creating asset [$asset] on file system at [$destFile]")

    try {
      FileUtils.writeByteArrayToFile(destFile, asset.data)
    }
    catch {
      case ex: IOException =>
        throw StorageException(s"Error creating [$asset] @ [$destFile]: $ex]")
    }
  }

  override def addPreview(preview: MimedPreviewData): Unit = {
    logger.info(s"Adding preview for asset ${preview.assetId}")

    // get the full path to our preview file
    val destFilePath = previewFilePath(preview.assetId)
    // parse out the dir path
    val dirPath = FilenameUtils.getFullPath(destFilePath)

//    FileUtils.forceMkdir(new File(dirPath))

    try {
      FileUtils.writeByteArrayToFile(new File(destFilePath), preview.data)
    }
    catch {
      case _: IOException => logger.error(s"Could not save preview data to [$destFilePath]")
    }
  }

  override def getPreviewById(assetId: String): MimedPreviewData = {
    val f: File = new File(previewFilePath(assetId))

    if (!f.isFile) {
      throw NotFoundException(s"Cannot find preview for asset '$assetId'")
    }

    val byteArray = FileUtils.readFileToByteArray(f)
    val is: InputStream = new ByteArrayInputStream(byteArray)

    try {
      MimedPreviewData(
        assetId = assetId,
        data = byteArray)
    }
    finally {
      if (is != null) is.close()
    }
  }

  private def previewFilePath(assetId: String): String = {
    val dirName = assetId.substring(0, 2)
    new File(new File(previewDataPath, dirName).getPath, s"$assetId.${MimedPreviewData.FILE_EXTENSION}").getPath
  }

  private def repositoryDataPath: String = {
    val reposDataPath = FilenameUtils.concat(app.dataPath, C.DataStore.REPOSITORIES)
    val repositoryDir = RequestContext.getRepository.persistedId
    FilenameUtils.concat(reposDataPath, repositoryDir)
  }

  private def previewDataPath: String =
    new File(repositoryDataPath, C.DataStore.PREVIEW).getPath

  private def filePath(assetId: String): String = {
    val filesPath = FilenameUtils.concat(repositoryDataPath, C.DataStore.FILES)
    val partitionedFilesPath = FilenameUtils.concat(filesPath, assetId.substring(0, 2))
    FilenameUtils.concat(partitionedFilesPath, assetId)
  }

  private def facePath(faceId: String): String = {
    val facesPath = FilenameUtils.concat(repositoryDataPath, C.DataStore.FACES)
    val partitionedFacesPath = FilenameUtils.concat(facesPath, faceId.substring(0, 2))
    FilenameUtils.concat(partitionedFacesPath, faceId)
  }

  override def addFace(face: Face): Unit = {
    val destFile = new File(facePath(face.persistedId))
    logger.debug(s"Creating face [$face] on file system at [$destFile]")

    try {
      FileUtils.writeByteArrayToFile(destFile, face.image)
    }
    catch {
      case ex: IOException =>
        throw StorageException(s"Error creating [$face] @ [$destFile]: $ex]")
    }
  }
}
