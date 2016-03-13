package altitude.dao

import java.io.File

import altitude.models.FileImportAsset
import altitude.{Altitude, Const => C}
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{IOFileFilter, TrueFileFilter}
import org.slf4j.LoggerFactory


class FileSystemImportDao(val app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)

  private val ANY_FILE_FILTER: IOFileFilter = TrueFileFilter.INSTANCE

  def iterateAssets(path: String): Iterator[FileImportAsset] = {
    require(path != null)
    log.info(s"Importing from '$path'", C.LogTag.DB)

    val files = FileUtils.iterateFiles(new File(path), ANY_FILE_FILTER, ANY_FILE_FILTER)
    new Iterable[FileImportAsset] {
      def iterator = new Iterator[FileImportAsset] {
        def hasNext = files.hasNext

        def next() = {
          val file: File = new File(files.next().toString)
          new FileImportAsset(file)
        }
      }
    }.toIterator
  }
}