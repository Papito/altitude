package software.altitude.core.controllers.htmx

import org.scalatra.Route
import play.api.libs.json.JsObject
import software.altitude.core.Validators.ApiRequestValidator
import software.altitude.core.{DataScrubber, DuplicateException, ValidationException}
import software.altitude.core.controllers.BaseHtmxController
import software.altitude.core.models.Person
import software.altitude.core.{Const => C}

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

  val searchPerson: Route = get("/r/:repoId/p/:personId/search") {
    val personId: String = params.get("personId").get
    val person: Person = app.service.person.getById(personId)

    ssp("htmx/person", "person" -> person)
  }

  val showEditPersonName: Route = get("/r/:repoId/p/:personId/name/edit") {
    val personId: String = params.get("personId").get
    val person: Person = app.service.person.getById(personId)

    ssp("htmx/edit_person_name", "person" -> person)
  }

  val editPersonName: Route = put("/r/:repoId/p/:personId/name/edit") {
    val dataScrubber = DataScrubber(
      trim = List(C.Api.Person.NAME),
    )

    val apiRequestValidator = ApiRequestValidator(
      required = List(
        C.Api.Person.NAME, C.Api.ID),
      maxLengths = Map(
        C.Api.Person.NAME -> C.Api.Constraints.MAX_NAME_LENGTH,
      ),
      minLengths = Map(
        C.Api.Person.NAME -> C.Api.Constraints.MIN_NAME_LENGTH,
      ),
    )

    val jsonIn: JsObject = dataScrubber.scrub(unscrubbedReqJson.get)
    val personId =  (jsonIn \ C.Api.ID).as[String]
    val person: Person = app.service.person.getById(personId)

    def haltWithValidationErrors(errors: Map[String, String]): Unit = {
      halt(200,
        ssp(
          "htmx/edit_person_name",
          "fieldErrors" -> errors,
          "formJson" -> jsonIn,
          "person" -> person,
          "newName" -> (jsonIn \ C.Api.Person.NAME).asOpt[String],
        ),
      )
    }

    try {
      apiRequestValidator.validate(jsonIn)
    } catch {
      case validationException: ValidationException =>
        haltWithValidationErrors(
          validationException.errors.toMap)
    }

    val newName = (jsonIn \ C.Api.Person.NAME).as[String]

    try {
      app.service.person.updateName(person, newName=newName)
    } catch {
      case ex: DuplicateException =>
        val message = ex.message.getOrElse("Person by that name already exists")
        haltWithValidationErrors(Map(C.Api.Person.NAME -> message))
    }

    val updatedPerson: Person = app.service.person.getById(personId)
    ssp("htmx/view_person_name", "person" -> updatedPerson)
  }
}
