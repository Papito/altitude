package software.altitude.core.service.sources

import java.io.File

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{IOFileFilter, TrueFileFilter}
import org.slf4j.LoggerFactory
import software.altitude.core.models._
import software.altitude.core.{Altitude, Const => C}

class FileSystemSourceService(app: Altitude) extends AssetSourceService {
  private final val log = LoggerFactory.getLogger(getClass)

  private val ANY_FILE_FILTER: IOFileFilter = TrueFileFilter.INSTANCE

  override def assetIterator(path: String): Iterator[ImportAsset] = {
    require(path != null)
    log.info(s"Importing from '$path'")

    val files = FileUtils.iterateFiles(new File(path), ANY_FILE_FILTER, ANY_FILE_FILTER)
    new Iterable[ImportAsset] {
      def iterator = new Iterator[ImportAsset] {
        def hasNext = files.hasNext

        def next() = fileToImportAsset(new File(files.next().toString))
      }
    }.toIterator
  }

  override def count(path: String): Int = {
    val itr = assetIterator(path)

    var count = 0

    while (itr.hasNext) {
      count += 1
      itr.next()
    }
    count
  }

  def fileToImportAsset(file: File): ImportAsset = new ImportAsset(
    fileName = file.getName,
    path = file.getAbsolutePath,
    sourceType = C.FileStoreType.FS,
    data =  FileUtils.readFileToByteArray(file),
    metadata = new Metadata())
}