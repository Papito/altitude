package unit

import altitude.models.search.Query
import org.scalatest.FunSuite

class SearchQueryModelTests extends FunSuite {

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
