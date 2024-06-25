package software.altitude.test.core.web

import org.scalatest.DoNotDiscover
import play.api.libs.json.Json
import software.altitude.core.{Const => C}
import software.altitude.test.core.HtmxTestCore

@DoNotDiscover class SetupControllerTests(val config: Map[String, Any]) extends HtmxTestCore {

  test("Should return validation errors") {
    post("/htmx/admin/setup", body = "{}", headers = getHeaders) {
      val requiredCount = "required".r.findAllIn(response.body).toList.size
      requiredCount should be (4)
    }
  }

  test("Should not allow mismatching passwords") {
    val payload = Json.obj(
      C.Api.Fields.ADMIN_EMAIL -> "me@me.com",
      C.Api.Fields.REPOSITORY_NAME -> "My Repository",
      C.Api.Fields.PASSWORD -> "password",
      C.Api.Fields.PASSWORD2 -> "oops"
    )
    post("/htmx/admin/setup", body = payload.toString(), headers = getHeaders) {
      response.body should include ("Passwords do not match")
    }
  }

  /* Since web tests spawn a new instance of the application, it messes with the state and transaction data.
     The spawned app is also Postgres for both Sqlite and Postgres tests, which makes it even worse.
     When I have time, I will have to figure out how to make web controller tests less brittle.
     https://trello.com/c/iM2OSgbm
  */

    test("Should successfully initialize when the form is valid", Focused) {
    val payload = Json.obj(
      C.Api.Fields.ADMIN_EMAIL -> "me@me.com",
      C.Api.Fields.REPOSITORY_NAME -> "My Repository",
      C.Api.Fields.PASSWORD -> "password3000",
      C.Api.Fields.PASSWORD2 -> "password3000"
    )

    post("/htmx/admin/setup", body = payload.toString(), headers = getHeaders) {
      status should equal(200)

      // Sends HTMX redirect to the root path
      response.getHeader("HX-Redirect") should be("/")
    }
  }
}
