package altitude.controllers

import altitude.models.ImportProfile
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

class ImportProfileApiController extends BaseController {
  val log =  LoggerFactory.getLogger(getClass)

  get("/:id") {
    val id = params("id")
    val importProfile: ImportProfile = app.service.importProfile.getById(id)

    Ok(importProfile.toJson)
  }

  post("/") {
    val name = params("name")
    val importProfile = new ImportProfile(name = name)
    log.debug(importProfile.toJson.toString())
    val newImportProfile: ImportProfile = app.service.importProfile.add(importProfile)

    Ok(Json.obj("importProfile" -> Map("id" -> newImportProfile.id)))
  }
}
