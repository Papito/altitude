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
      name = "Holden McGroin 1",
    )
    val savedPerson1: Person = testApp.service.person.add(person)

    val person2 = Person(
      name = "Holden McGroin 2",
    )
    val savedPerson2: Person = testApp.service.person.add(person2)

    savedPerson1.label should be(1)
    savedPerson2.label should be(2)
  }
}
