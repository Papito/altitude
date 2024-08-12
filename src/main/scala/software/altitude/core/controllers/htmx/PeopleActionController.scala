package software.altitude.core.controllers.htmx

import org.scalatra.Route
import software.altitude.core.controllers.BaseHtmxController
import software.altitude.core.models.Person

/**
  * @ /htmx/people/
 */
class PeopleActionController extends BaseHtmxController{

  before() {
    requireLogin()
  }

  val showPeopleTab: Route = get("/r/:repoId/tab") {
    val people: List[Person] = app.service.person.getAll

    ssp("htmx/people", "people" -> people)
  }

}
