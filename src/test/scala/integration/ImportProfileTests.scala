package integration

import altitude.exceptions.ValidationException
import altitude.models.ImportProfile
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import play.api.libs.json.JsNull

@DoNotDiscover class ImportProfileTests(val config: Map[String, String]) extends IntegrationTestCore {
  test("Missing profile name") {
    val importProfile: ImportProfile = new ImportProfile(name = "", tagData = JsNull)

    intercept[ValidationException] {
      altitude.service.importProfile.add(importProfile)
    }
  }

  test("Empty profile name") {
    val importProfile: ImportProfile = new ImportProfile(name = "   \t \n", tagData = JsNull)

    intercept[ValidationException] {
      altitude.service.importProfile.add(importProfile)
    }
  }

  test("Good profile") {
    val importProfile: ImportProfile = new ImportProfile(name = "test", tagData = JsNull)
    altitude.service.importProfile.add(importProfile)
  }
}
