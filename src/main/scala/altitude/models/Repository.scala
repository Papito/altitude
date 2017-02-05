package altitude.models

import altitude.{Const => C}
import play.api.libs.json._

import scala.collection.immutable.HashMap
import scala.language.implicitConversions


object Repository {
  implicit def fromJson(json: JsValue): Repository = Repository(
    id = (json \ C.Base.ID).asOpt[String],
    name = (json \ C.Repository.NAME).as[String],
    rootFolderId = (json \ C.Repository.ROOT_FOLDER_ID).as[String],
    uncatFolderId = (json \ C.Repository.UNCAT_FOLDER_ID).as[String],
    fileStoreType = C.FileStoreType.withName((json \ C.Repository.FILE_STORE_TYPE).as[String]),
    fileStoreConfig = new HashMap[String, String]()
  )

  implicit def toJson(repo: Repository): JsObject = repo.toJson
}

case class Repository(id: Option[String] = None,
                      name: String,
                      rootFolderId: String,
                      uncatFolderId: String,
                      fileStoreType: C.FileStoreType.Value,
                      fileStoreConfig: Map[String, String]) extends BaseModel {

  def toJson = {
    Json.obj(
      C.Repository.ID -> id,
      C.Repository.NAME -> name,
      C.Repository.ROOT_FOLDER_ID -> rootFolderId,
      C.Repository.UNCAT_FOLDER_ID -> uncatFolderId,
      C.Repository.FILE_STORE_TYPE -> fileStoreType.toString
    )
  }
}

