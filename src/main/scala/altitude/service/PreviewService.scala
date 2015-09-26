package altitude.service

import java.io.File

import altitude.Altitude
import altitude.models.Preview
import org.slf4j.LoggerFactory
import org.apache.commons.io.{FileUtils, FilenameUtils}

class PreviewService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)

  def add(preview: Preview): Unit = {
    log.info(s"Adding preview for asset ${preview.assetId}")

    val filePath = previewFilePath(preview.assetId)
    val dirPath = FilenameUtils.getFullPath(filePath)
    log.info(dirPath)
    FileUtils.forceMkdir(new File(dirPath))
  }

  def getById(id: String): Preview = {
    throw new NotImplementedError()
  }

  private def previewFilePath(assetId: String): String = {
    val dirName = assetId.substring(0, 3)
    app.previewPath + dirName + "/" + assetId + ".jpg"
  }
}
