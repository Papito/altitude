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
    val pathComponents: List[String] = currentPathFile.getAbsolutePath.split(File.separator).toList
    // drop path components from the back of the path, based on the final index we want to jump to
    val newPathComponents: List[String] = pathComponents.drop(pathComponents.size - position - 1)
    log.info(newPathComponents.toString())
    val jumpToDirPath = Paths.get("", newPathComponents: _*)
    log.info(jumpToDirPath.toAbsolutePath.toString)
    getDirectoryListing(jumpToDirPath.toFile)
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
