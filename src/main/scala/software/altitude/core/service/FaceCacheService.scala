package software.altitude.core.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Altitude
import software.altitude.core.RequestContext
import software.altitude.core.models.Face
import software.altitude.core.models.Person

import scala.collection.mutable


class FaceCacheService(app: Altitude) {
  protected final val logger: Logger = LoggerFactory.getLogger(getClass)

  private type Label = Int
  private type PersonCache = mutable.Map[Label, Person]
  private type RepositoryPersonCache = mutable.Map[String, PersonCache]

  // Model label (int) -> Person
  private def newPersonCache(): PersonCache = mutable.Map[Label, Person]()

  // Repo ID -> Person Cache
  private val cache: RepositoryPersonCache = mutable.Map[String, PersonCache]()

  private def getRepositoryPersonCache: PersonCache = {
    cache.getOrElseUpdate(RequestContext.getRepository.persistedId, newPersonCache())
  }

  def getPersonByLabel(label: Label): Option[Person] = {
    val personOpt = getRepositoryPersonCache.get(label)

    // if this person had been merged - return the merge destination instead
    if (personOpt.nonEmpty && personOpt.get.mergedIntoLabel.nonEmpty) {
      logger.info(s"Person ${personOpt.get.label} was merged into ${personOpt.get.mergedIntoLabel.get}")
      logger.info("Returning the merged person instead")
      return getRepositoryPersonCache.get(personOpt.get.mergedIntoLabel.get)
    }

    if (personOpt.isEmpty) {
      logger.warn(s"Person with label ${label} not found in cache")
      None
    } else {
      logger.info(s"Returning label ${label} from cache: ${personOpt.get.name}")
      personOpt
    }
  }

  def putPerson(person: Person): Unit = {
    // A person that was not merged cannot be empty.
    // Probably operator error not correctly constructing the person object.
    if (!person.hasFaces && !person.wasMergedFrom) {
      throw new IllegalArgumentException(s"Person ${person.label} has no faces attached to it")
    }

    getRepositoryPersonCache.put(person.label, person)
  }

  def addFace(face: Face): Person = {
    val person = getRepositoryPersonCache(face.personLabel.get)

    /**
     * Add the face and trim the number to top X faces - we don't need all of them.
     * Note that we are not getting cute here with checking the list size or anything -
     * the list is sorted by detection score, so it must be pruned AFTER the sort settles.
     */
    val faces = person.getFaces
    faces.add(face)
    person.setFaces(faces.take(FaceRecognitionService.MAX_COMPARISONS_PER_PERSON).toSeq)

    // keep true to the actual number of faces
    val updatedPerson = person.copy(numOfFaces = person.numOfFaces + 1)
    updatedPerson.setFaces(person.getFaces.toSeq)
    putPerson(updatedPerson)
    updatedPerson
  }

  def removePerson(label: Label): Unit = {
    getRepositoryPersonCache.remove(label)
  }

  def size(): Int = getRepositoryPersonCache.keys.size

  def getAll: List[Person] = getRepositoryPersonCache.values.toList

  def dump(): Unit = {
    cache.foreach { case (repoId, personCache) =>
      println(s"Repository: $repoId\n")
      personCache.foreach { case (label, person) =>
        println(s"  ID: ${person.persistedId}")
        println(s"  Label: $label -> ${person.name.get}")
        println(s"  Merged with IDs: ${person.mergedWithIds}")
        println(s"  Merged into: ${person.mergedIntoId.getOrElse("Nothing")}, ${person.mergedIntoLabel.getOrElse("Nothing")}")

        if (person.hasFaces) {
          person.getFaces.foreach { face =>
            println(s"    Face ${face.id} -> ${face.detectionScore}")
          }
        } else {
          println("    Person has no faces stored in cache")
        }
        println()
      }
    }
  }
}
