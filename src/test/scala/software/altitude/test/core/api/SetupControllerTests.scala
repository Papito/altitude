package software.altitude.test.core.api

import org.scalatest.DoNotDiscover
import play.api.libs.json.Json
import software.altitude.core.{Const => C}

@DoNotDiscover class SetupControllerTests(val config: Map[String, Any]) extends ApiTestCore {

  test("Should return validation errors") {
    post("/api/v1/admin/setup", body = "{}", headers = getHeaders) {
      (jsonResponse.get \ C.Api.Fields.FIELD_ERRORS \ C.Api.Fields.ADMIN_EMAIL).asOpt[String] should not be(None)
      (jsonResponse.get \ C.Api.Fields.FIELD_ERRORS \ C.Api.Fields.REPOSITORY_NAME).asOpt[String] should not be(None)
      (jsonResponse.get \ C.Api.Fields.FIELD_ERRORS \ C.Api.Fields.PASSWORD).asOpt[String] should not be(None)
      (jsonResponse.get \ C.Api.Fields.FIELD_ERRORS \ C.Api.Fields.PASSWORD2).asOpt[String] should not be(None)
    }
  }


  test("Should not allow mismatching passwords") {
    val payload = Json.obj(
      C.Api.Fields.ADMIN_EMAIL -> "me@me.com",
      C.Api.Fields.REPOSITORY_NAME -> "My Repository",
      C.Api.Fields.PASSWORD -> C.Api.Fields.PASSWORD,
      C.Api.Fields.PASSWORD2 -> "oops"
    )
    post("/api/v1/admin/setup", body = payload.toString(), headers = getHeaders) {
      (jsonResponse.get \ C.Api.Fields.FIELD_ERRORS \ C.Api.Fields.ADMIN_EMAIL).asOpt[String] should be(None)
      (jsonResponse.get \ C.Api.Fields.FIELD_ERRORS \ C.Api.Fields.REPOSITORY_NAME).asOpt[String] should be(None)
      (jsonResponse.get \ C.Api.Fields.FIELD_ERRORS \ C.Api.Fields.PASSWORD2).asOpt[String] should be(None)
      // only the primary password field should have an error
      (jsonResponse.get \ C.Api.Fields.FIELD_ERRORS \ C.Api.Fields.PASSWORD).as[String] should be(C.Msg.Err.PASSWORDS_DO_NOT_MATCH)
    }
  }
}
