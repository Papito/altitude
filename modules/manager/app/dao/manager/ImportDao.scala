package dao.manager

import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{IOFileFilter, TrueFileFilter}
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

import models.ImportAsset
import util.log
import constants.{const => C}

class ImportDao {
  private val ANY_FILE_FILTER: IOFileFilter = TrueFileFilter.INSTANCE

  def iterateAssets(path: String): Iterator[ImportAsset] = {
    require(path != null)
    log.info("Importing from 'importPath'", Map("importPath" -> path), C.tag.DB)

    val files = FileUtils.iterateFiles(new File(path), ANY_FILE_FILTER, ANY_FILE_FILTER)

    new Iterable[ImportAsset] {
      def iterator = new Iterator[ImportAsset] {
        def hasNext = files.hasNext

        def next() = {
          val file: File = new File(files.next().toString)
          log.info("Processing '$file'", Map("file" -> file), C.tag.DB)
          new ImportAsset(file)
        }
      }
    }.toIterator
  }
}