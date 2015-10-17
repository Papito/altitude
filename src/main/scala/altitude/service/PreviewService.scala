package altitude.service

import java.io._

import altitude.Altitude
import altitude.exceptions.NotFoundException
import altitude.models.{MediaType, Preview}
import org.slf4j.LoggerFactory
import altitude.{Const => C}
import org.apache.commons.io.{FileUtils, FilenameUtils}

class PreviewService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)

  def add(preview: Preview): Unit = {
    log.info(s"Adding preview for asset ${preview.assetId}")

    // get the full path to our preview file
    val filePath = previewFilePath(preview.assetId)
    // parse out the dir path (we may need to create it)
    val dirPath = FilenameUtils.getFullPath(filePath)
    log.debug(dirPath)

    FileUtils.forceMkdir(new File(dirPath))

    val bos = new BufferedOutputStream(new FileOutputStream(filePath))

    try {
      Stream.continually(bos.write(preview.data))
    }
    finally {
      if (bos != null) bos.close()
    }
  }

  def getById(assetId: String): Preview = {
    val f: File = new File(previewFilePath(assetId))

    if (!f.isFile) {
      throw new NotFoundException(C.IdType.ID, assetId)
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

  private def previewFilePath(assetId: String): String = {
    val dirName = assetId.substring(0, 3)
    app.previewPath + dirName + "/" + assetId + ".jpg"
  }
}
