package altitude.service.sources

import java.io.{File, FileInputStream, InputStream}

import altitude.exceptions.{FormatException, MetadataExtractorException}
import altitude.models._
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{TrueFileFilter, IOFileFilter}
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{Metadata => TikaMetadata}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}

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
    path = file.getAbsolutePath,
    sourceType = StorageType.FS,
    data =  FileUtils.readFileToByteArray(file),
    metadata = new Metadata())
}