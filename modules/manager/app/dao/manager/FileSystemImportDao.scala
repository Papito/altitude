package dao.manager

import java.io.File

import constants.{const => C}
import models.manager.FileImportAsset
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.{IOFileFilter, TrueFileFilter}
import util.log

class FileSystemImportDao {
  private val ANY_FILE_FILTER: IOFileFilter = TrueFileFilter.INSTANCE

  def iterateAssets(path: String): Iterator[FileImportAsset] = {
    require(path != null)
    log.info("Importing from 'importPath'", Map("importPath" -> path), C.tag.DB)

    val files = FileUtils.iterateFiles(new File(path), ANY_FILE_FILTER, ANY_FILE_FILTER)

    new Iterable[FileImportAsset] {
      def iterator = new Iterator[FileImportAsset] {
        def hasNext = files.hasNext

        def next() = {
          val file: File = new File(files.next().toString)
          //log.info("Found file '$file'", Map("file" -> file), C.tag.DB)
          new FileImportAsset(file)
        }
      }
    }.toIterator
  }
}