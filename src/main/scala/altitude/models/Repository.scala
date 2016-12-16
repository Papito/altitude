package altitude.models

import altitude.{Const => C}
import play.api.libs.json._

import scala.language.implicitConversions


object Repository {
  implicit def fromJson(json: JsValue): Repository = Repository(
    id = (json \ C.Base.ID).asOpt[String],
    (json \ C.Repository.NAME).as[String],
    (json \ C.Repository.ROOT_FOLDER_ID).as[String],
    (json \ C.Repository.UNCAT_FOLDER_ID).as[String]
  )

  implicit def toJson(repo: Repository): JsObject = repo.toJson
}

case class Repository(id: Option[String] = None,
                      name: String,
                      rootFolderId: String,
                      uncatFolderId: String) extends BaseModel {

  def toJson = {
    Json.obj(
      C.Repository.ID -> id,
      C.Repository.NAME -> name,
      C.Repository.ROOT_FOLDER_ID -> rootFolderId,
      C.Repository.UNCAT_FOLDER_ID -> uncatFolderId
    )
  }
}

