package altitude.controllers.api.admin

import altitude.controllers.api.BaseApiController
import altitude.{Const => C}
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, JsArray}

class MetadataController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    val allMetadataFields = app.service.metadata.getAllFields.values

    Ok(Json.obj(
      C.Api.Metadata.FIELDS -> JsArray(allMetadataFields.map(_.toJson).toSeq)
    ))
  }

  post("/") {
    val name = (requestJson.get \ C.Api.Metadata.Field.NAME).as[String]
    val fieldType = (requestJson.get \ C.Api.Metadata.Field.TYPE).as[String]
    log.info(s"Adding metadata field [$name] of type [$fieldType]")

    OK
  }

  delete("/:id") {
    val id = params.get(C.Api.ID).get
    log.info(s"Deleting metadata field: $id")

    OK
  }

  put("/:id") {
    val id = params.get(C.Api.ID).get
    log.info(s"Deleting metadata field: $id")

    OK
  }

}
