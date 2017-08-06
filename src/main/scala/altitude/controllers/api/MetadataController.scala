package altitude.controllers.api

import altitude.models.{FieldType, MetadataField}
import altitude.{Const => C}
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsArray, Json}

class MetadataController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/") {
    val allMetadataFields = app.service.metadata.getAllFields.values

    Ok(Json.obj(
      C.Api.Metadata.FIELDS -> JsArray(allMetadataFields.map(_.toJson).toSeq)
    ))
  }
}
