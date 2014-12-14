package dao.manager

import java.io.File

import constants.{const => C}
import models.ImportAsset
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{IOFileFilter, TrueFileFilter}
import util.log

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
          log.info("Found file '$file'", Map("file" -> file), C.tag.DB)
          new ImportAsset(file)
        }
      }
    }.toIterator
  }
}