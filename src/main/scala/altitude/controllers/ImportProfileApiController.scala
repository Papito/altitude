package altitude.controllers

import altitude.models.ImportProfile
import altitude.models.search.Query
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import altitude.{Const => C}

class ImportProfileApiController extends BaseController {
  val log =  LoggerFactory.getLogger(getClass)

  get("/:id") {
    val id = params(C.Api.ID)
    val importProfile: ImportProfile = app.service.importProfile.getById(id)

    Ok(importProfile.toJson)
  }

  get("/") {
    val jsonImportProfiles = app.service.importProfile.query(new Query())
    Ok(Json.obj(C.Api.ImportProfile.IMPORT_PROFILES -> jsonImportProfiles))
  }

  post("/") {
    val name = params(C.Api.ImportProfile.NAME)
    val importProfile = new ImportProfile(name = name)
    log.debug(importProfile.toJson.toString())
    val newImportProfile: ImportProfile = app.service.importProfile.add(importProfile)

    Ok(Json.obj(C.Api.ID -> newImportProfile.id))
  }
}
