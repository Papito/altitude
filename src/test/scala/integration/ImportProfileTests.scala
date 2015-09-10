package integration

import altitude.exceptions.ValidationException
import altitude.models.ImportProfile
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class ImportProfileTests(val config: Map[String, String]) extends IntegrationTestCore {
  test("Profile validation") {
    val importProfile: ImportProfile = new ImportProfile(name = "")

    intercept[ValidationException] {
      altitude.service.importProfile.add(importProfile)
    }
  }
}
