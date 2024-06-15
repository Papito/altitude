package software.altitude.core.controllers.api.navigate

import org.scalatra.CorsSupport
import org.scalatra.InternalServerError
import org.scalatra.ScalatraServlet
import org.slf4j.LoggerFactory
import play.api.libs.json._
import software.altitude.core.{Const => C}

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Paths

class FileSystemBrowserController extends ScalatraServlet with CorsSupport {
  private final val log = LoggerFactory.getLogger(getClass)
  private val userHomeDir = System.getProperty("user.home")

  // OPTIMIZE: Does this even work in Windows?

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
    val allContents: Seq[File] = file.listFiles().toSeq

    val files: Seq[String] = allContents
      .filter(_.isFile)
      .map(_.getName)
      .filter(!_.startsWith("."))
      .sorted

    val directories: Seq[String] = allContents
      .filter(_.isDirectory)
      .map(_.getName)
      .filter(!_.startsWith("."))
      .sorted

    // FIXME: bush league - use proper Path ways to do this
    val pathBreadcrumbs = file.getAbsolutePath.split(File.separator.toCharArray).filter(!_.isBlank).toList

    Json.obj(
      C.Api.FILES -> files,
      C.Api.DIRECTORIES -> directories,
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