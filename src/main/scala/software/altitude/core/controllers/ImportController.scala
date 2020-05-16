package software.altitude.core.controllers

import org.scalatra.{CorsSupport, InternalServerError, ScalatraServlet}
import java.io.{File, PrintWriter, StringWriter}

import org.slf4j.LoggerFactory
import play.api.libs.json._
import software.altitude.core.{Context, DuplicateException, MetadataExtractorException, Const => C}

//noinspection DuplicatedCode
class ImportController extends ScalatraServlet with CorsSupport {
  private final val log = LoggerFactory.getLogger(getClass)
  private val userHomeDir = System.getProperty("user.home")

  get("/fs/listing") {
    val file: File = new File(this.params.getOrElse(C.Api.PATH, userHomeDir))

    log.debug(s"Getting directory name list for $file")
    val files: Seq[File] = file.listFiles().toSeq
    val directoryList: Seq[String] = files
      .filter(f => f.isDirectory && !f.getName.startsWith("."))
      .map(_.getName)
      .sorted

    contentType = "application/json"
    Json.obj(
      C.Api.DIRECTORY_LIST -> directoryList,
      C.Api.CURRENT_PATH -> file.getAbsolutePath,
      C.Api.OS_PATH_SEPARATOR -> File.separator).toString()
  }

  get("/fs/listing/parent") {
    val path = this.params.getOrElse(C.Api.PATH, userHomeDir)
    log.debug(s"Path: $path")
    val file: File = new File(this.params.getOrElse(C.Api.PATH, userHomeDir))
    val parentPath: String = file.getParent
    log.debug(s"Parent path: $parentPath")

    val parentFile = if (parentPath != null) new File(parentPath) else file

    log.debug(s"Getting parent directory name list for $file")
    val files: Seq[File] = parentFile.listFiles().toSeq
    val directoryList: Seq[String] = files.filter(_.isDirectory == true).map(_.getName)
    contentType = "application/json"
    Json.obj(
      C.Api.DIRECTORY_LIST -> directoryList,
      C.Api.CURRENT_PATH -> parentFile.getAbsolutePath).toString()
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
