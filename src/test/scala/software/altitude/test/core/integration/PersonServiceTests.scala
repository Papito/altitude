package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.Altitude
import software.altitude.core.models.Person
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class PersonServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  test("Can add and retrieve a person", Focused) {
    val person = Person(
      name = "Holden McGroin",
    )
    val savedPerson1: Person = testApp.service.person.add(person)
    val retrievedPerson1: Person = testApp.service.person.getById(savedPerson1.persistedId)
    retrievedPerson1.isHidden should be(false)
    retrievedPerson1.name should be(person.name)

    val person2 = Person(
      name = "Justin Case",
    )
    val savedPerson2: Person = testApp.service.person.add(person2)

    savedPerson2.label - savedPerson1.label should be(1)

  }
}
