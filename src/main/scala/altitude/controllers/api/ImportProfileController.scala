package altitude.controllers.api

import altitude.controllers.BaseController
import altitude.models.ImportProfile
import altitude.models.search.Query
import altitude.service.TagConfigService
import altitude.{Const => C}
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsNull, JsArray, JsValue, Json}

class ImportProfileController extends BaseApiController {
  override val log =  LoggerFactory.getLogger(getClass)

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
    val name = params.getOrElse(C.Api.ImportProfile.NAME, "")
    val keywords = params.getOrElse(C.ImportProfile.KEYWORDS, "")

    //TODO: parse keywords with a utility
    val keywordList = List(keywords)

    val tagData: JsValue = keywordList.isEmpty match {
      case true => JsArray(Seq(
        Json.obj(C.Api.ID -> TagConfigService.KEYWORDS_TAG_ID, C.Api.DATA -> JsNull)
      ))
      case false => JsArray(Seq(
          Json.obj(C.Api.ID -> TagConfigService.KEYWORDS_TAG_ID, C.Api.DATA -> keywordList)
      ))
    }



    val importProfile = new ImportProfile(name = name, tagData = tagData)
    log.debug(importProfile.toJson.toString())
    val newImportProfile: ImportProfile = app.service.importProfile.add(importProfile)

    Ok(Json.obj(C.Api.ID -> newImportProfile.id))
  }
}
