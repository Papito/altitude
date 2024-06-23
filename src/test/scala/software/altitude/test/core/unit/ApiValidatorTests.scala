package software.altitude.test.core.unit

import org.scalatest.funsuite
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.Json
import software.altitude.core.ValidationException
import software.altitude.core.Validators.ApiRequestValidator
import software.altitude.core.{Const => C}
import software.altitude.test.core.TestFocus


class ApiValidatorTests extends funsuite.AnyFunSuite with TestFocus {

  test("Test that all types of errors come back at once", Focused) {
    val validator: ApiRequestValidator = ApiRequestValidator(
      required=Option(List(C.Api.ID, C.Api.Folder.NAME))
    )

    val jsonIn = Json.obj(
      C.Api.Folder.PATH -> "Bright Future Path"
    )

    val validationException = intercept[ValidationException] {
      validator.validate(jsonIn)
    }

    validationException.errors.size should be(validator.required.get.size)
  }

}
