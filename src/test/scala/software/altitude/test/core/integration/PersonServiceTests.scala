package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.core.Altitude
import software.altitude.core.models.Person
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class PersonServiceTests(override val testApp: Altitude) extends IntegrationTestCore {

  test("Can add and retrieve a person") {
    val person1Model = Person()
    val person1: Person = testApp.service.person.add(person1Model)

    val retrievedPerson1: Person = testApp.service.person.getById(person1.persistedId)
    retrievedPerson1.isHidden should be(false)

    val person2Model = Person()
    val person2: Person = testApp.service.person.add(person2Model)
    val retrievedPerson2: Person = testApp.service.person.getById(person2.persistedId)

    retrievedPerson2.label - retrievedPerson1.label should be(1)
  }

  test("Can merge three people into one") {
    val person1Model = Person()
    val person1: Person = testApp.service.person.add(person1Model)

    val person2Model = Person()
    val person2: Person = testApp.service.person.add(person2Model)

    val mergedIntoPerson: Person = testApp.service.person.merge(dest=person1, source=person2)
    val savedMergedIntoPerson: Person = testApp.service.person.getById(person1.persistedId)

    mergedIntoPerson.mergedWithIds.size should be(1)
    mergedIntoPerson.mergedWithIds should be(savedMergedIntoPerson.mergedWithIds)

    val savedMergedPerson: Person = testApp.service.person.getById(person2.persistedId)
    savedMergedPerson.mergedIntoId should be(Some(mergedIntoPerson.persistedId))

    val person3Model = Person()
    val person3: Person = testApp.service.person.add(person3Model)

    /**
     * To completely make sure this works, merge a second person into the OG destination (person1)
     *
     * The returned person from the merge() operation should identical to the persisted version, when we
     * get that object by ID directly.
     */
    val mergedIntoPersonAgain: Person = testApp.service.person.merge(dest=mergedIntoPerson, source=person3)
    mergedIntoPersonAgain.mergedWithIds.size should be(2)

    val savedMergedIntoPersonAgain: Person = testApp.service.person.getById(mergedIntoPerson.persistedId)
    mergedIntoPersonAgain.mergedWithIds should be(savedMergedIntoPersonAgain.mergedWithIds)
  }
}
