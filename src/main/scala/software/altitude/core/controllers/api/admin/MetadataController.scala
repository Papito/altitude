package software.altitude.core.controllers.api.admin

import org.slf4j.LoggerFactory
import software.altitude.core.controllers.api.BaseApiController
import software.altitude.core.models.{FieldType, MetadataField}
import software.altitude.core.{Const => C}

class MetadataController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  post("/") {
    val name = (requestJson.get \ C.Api.Metadata.Field.NAME).as[String]
    val fieldType = (requestJson.get \ C.Api.Metadata.Field.TYPE).as[String]
    log.info(s"Adding metadata field [$name] of type [$fieldType]")

    val newField = MetadataField(name = name, fieldType = FieldType.withName(fieldType.toUpperCase))

    app.service.metadata.addField(newField)

    OK
  }

  delete("/:id") {
    val id = params.get(C.Api.ID).get
    log.info(s"Deleting metadata field: $id")

    OK
  }

  put("/:id") {
    val id = params.get(C.Api.ID).get
    log.info(s"Updating metadata field: $id")

    OK
  }

}
