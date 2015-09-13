package altitude.controllers.api

import altitude.Validators.ApiValidator
import altitude.models.ImportProfile
import altitude.models.search.Query
import altitude.service.TagConfigService
import altitude.{Const => C}
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsArray, JsValue, Json}

class ImportProfileController extends BaseApiController {
  override val log =  LoggerFactory.getLogger(getClass)

  override val POST_VALIDATOR = Some(ApiValidator(List(
    C.Api.ImportProfile.NAME, C.Api.ImportProfile.KEYWORDS
  )))

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
    val name = params.get(C.Api.ImportProfile.NAME)
    val keywords = params.get(C.Api.ImportProfile.KEYWORDS)

    //TODO: parse keywords with a utility
    val keywordList = List(keywords)

    val tagData: JsValue = JsArray(Seq(
          Json.obj(C.Tag.ID -> TagConfigService.KEYWORDS_TAG_ID, C.ImportProfile.VALUES -> keywordList)
    ))

    val importProfile = new ImportProfile(name = name.get, tagData = tagData)
    log.debug(importProfile.toJson.toString())
    val newImportProfile: ImportProfile = app.service.importProfile.add(importProfile)

    Ok(Json.obj(C.Api.ID -> newImportProfile.id))
  }
}
