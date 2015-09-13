package integration

import altitude.{Const => C}
import altitude.exceptions.ValidationException
import altitude.models.ImportProfile
import altitude.service.TagConfigService
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import integration.util.Text
import play.api.libs.json.{Json, JsArray, JsNull}

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

  test("NULL Json tag data") {
    val importProfile: ImportProfile = new ImportProfile(name = "test", tagData = JsNull)

    intercept[ValidationException] {
      altitude.service.importProfile.add(importProfile)
    }
  }

  test("Empty tag data") {
    val importProfile: ImportProfile = new ImportProfile(name = Text.randomStr(), tagData = JsArray())

    intercept[ValidationException] {
      altitude.service.importProfile.add(importProfile)
    }
  }

  test("Good profile") {
    val importProfile: ImportProfile = new ImportProfile(
      name = Text.randomStr(),
      tagData = JsArray(Seq(
        Json.obj(C.Tag.ID -> TagConfigService.KEYWORDS_TAG_ID, C.ImportProfile.VALUES -> List("k1", "k2"))
      )))
    altitude.service.importProfile.add(importProfile)
  }
}
