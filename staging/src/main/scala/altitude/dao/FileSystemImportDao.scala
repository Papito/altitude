package altitude.dao

import java.io.File

import altitude.Util.log
import altitude.models.FileImportAsset
import altitude.models.search.Query
import altitude.{Const => C}
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{IOFileFilter, TrueFileFilter}
import play.api.libs.json.JsObject

class FileSystemImportDao extends BaseDao {
  private val ANY_FILE_FILTER: IOFileFilter = TrueFileFilter.INSTANCE

  def iterateAssets(path: String): Iterator[FileImportAsset] = {
    require(path != null)
    log.info(s"Importing from '$path'", C.tag.DB)

    // FIXME: use a future and a separate executor context
    val files = FileUtils.iterateFiles(new File(path), ANY_FILE_FILTER, ANY_FILE_FILTER)
    new Iterable[FileImportAsset] {
      def iterator = new Iterator[FileImportAsset] {
        def hasNext = files.hasNext

        def next() = {
          val file: File = new File(files.next().toString)
          //log.info(s"Found file '$file'", C.tag.DB)
          new FileImportAsset(file)
        }
      }
    }.toIterator
  }

  override def add(json: JsObject)(implicit txId: TransactionId): JsObject = throw new NotImplementedError
  override def getById(id: String)(implicit txId: TransactionId): Option[JsObject] = throw new NotImplementedError
  override def query(q: Query)(implicit txId: TransactionId): List[JsObject] = throw new NotImplementedError
}