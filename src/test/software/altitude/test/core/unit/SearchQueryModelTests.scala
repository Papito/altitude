package software.altitude.test.core.unit

import org.scalatest.FunSuite
import software.altitude.core.models.{BaseModel, User}
import software.altitude.core.util.Query

class SearchQueryModelTests extends FunSuite {

  var user: User = User(id = Some(BaseModel.genId))

  test("Invalid RPP") {
    intercept[IllegalArgumentException] {
      Query(rpp = -1)
    }
  }

  test("Invalid page") {
    intercept[IllegalArgumentException] {
      Query(page = 0)
    }
    intercept[IllegalArgumentException] {
      Query(page = -1)
    }
  }
}
