package unit

import altitude.models.User
import altitude.models.search.Query
import org.scalatest.FunSuite

class SearchQueryModelTests extends FunSuite {

  var user: User = User(id = Some("1"))

  test("Invalid RPP") {
    intercept[IllegalArgumentException] {
      Query(user, rpp = -1)
    }
  }

  test("Invalid page") {
    intercept[IllegalArgumentException] {
      Query(user, page = 0)
    }
    intercept[IllegalArgumentException] {
      Query(user, page = -1)
    }
  }
}
