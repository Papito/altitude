package software.altitude.core.controllers.api.navigate

import java.io.{File, PrintWriter, StringWriter}
import java.nio.file.Paths

import org.scalatra.{CorsSupport, InternalServerError, ScalatraServlet}
import org.slf4j.LoggerFactory
import play.api.libs.json._
import software.altitude.core.{Const => C}

class FileSystemBrowserController extends ScalatraServlet with CorsSupport {
  private final val log = LoggerFactory.getLogger(getClass)
  private val userHomeDir = System.getProperty("user.home")

  before() {
    contentType = "application/json; charset=UTF-8"
  }

  get("/fs/listing") {
    val currentPathFile: File = new File(params.getOrElse(C.Api.PATH, userHomeDir))
    getDirectoryListing(currentPathFile)
  }

  get("/fs/listing/parent") {
    val currentPathFile = new File(params.getOrElse(C.Api.PATH, ""))
    val parentDir: String = currentPathFile.getParent
    val parentDirFile = if (parentDir != null) new File(parentDir) else currentPathFile
    getDirectoryListing(parentDirFile)
  }

  get("/fs/listing/child") {
    val currentPathFile: File = new File(params.getOrElse(C.Api.PATH, ""))
    val childDirName = params.get(C.Api.CHILD_DIR)
    val childDirPath = Paths.get(currentPathFile.getAbsolutePath, childDirName.get)
    getDirectoryListing(childDirPath.toFile)
  }

  get("/fs/listing/jump") {
    val currentPathFile: File = new File(params.getOrElse(C.Api.PATH, ""))
    val position = params.getOrElse(C.Api.PATH_POS, "1").toInt
    val paths: List[File] = Iterator.iterate(currentPathFile)(_.getParentFile).takeWhile(_ != null).toList.reverse
    getDirectoryListing(paths(position + 1))
  }

  private def getDirectoryListing(file: File): JsObject = {
    log.debug(s"Getting directory name list for $file")
    val files: Seq[File] = file.listFiles().toSeq
    val directoryList: Seq[String] = files
      .filter(f => f.isDirectory && !f.getName.startsWith("."))
      .map(_.getName)
      .sorted

    val pathBreadcrumbs = file.getAbsolutePath.split(File.separator.toCharArray).filter(!_.isBlank).toList
    Json.obj(
      C.Api.DIRECTORY_LIST -> directoryList,
      C.Api.CURRENT_PATH -> file.getAbsolutePath,
      C.Api.CURRENT_PATH_BREADCRUMBS -> pathBreadcrumbs
    )
  }

  error {
    case ex: Exception =>
      ex.printStackTrace()
      val sw: StringWriter = new StringWriter()
      val pw: PrintWriter = new PrintWriter(sw)
      ex.printStackTrace(pw)
      log.error(s"Exception ${sw.toString}")
      InternalServerError(sw.toString)
  }
}
