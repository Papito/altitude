package altitude.service

import java.io._

import altitude.exceptions.NotFoundException
import altitude.models.Preview
import altitude.{Const => C, Context, Altitude}
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.slf4j.LoggerFactory

class PreviewService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)

  def add(preview: Preview)
         (implicit ctx: Context): Unit = {
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

  def getById(assetId: String)
             (implicit ctx: Context): Preview = {
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

  private def previewFilePath(assetId: String): String = {
    val dirName = assetId.substring(0, 3)
    app.previewPath + dirName + "/" + assetId + ".jpg"
  }
}
