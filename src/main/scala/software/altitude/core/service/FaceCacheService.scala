package software.altitude.core.service

import org.slf4j.{Logger, LoggerFactory}
import software.altitude.core.Altitude
import software.altitude.core.RequestContext
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
      logger.info(s"Returning the merged person instead")
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
    getRepositoryPersonCache.put(person.label, person)
  }

  def replace(person: Person): Unit = {
    getRepositoryPersonCache.put(person.label, person)
  }

  def removePerson(label: Label): Unit = {
    getRepositoryPersonCache.remove(label)
  }

  def size(): Int = getRepositoryPersonCache.keys.size

  def getAll: List[Person] = getRepositoryPersonCache.values.toList

  def dump(): Unit = {
    cache.foreach { case (repoId, personCache) =>
      println(s"Repository $repoId")
      personCache.foreach { case (label, person) =>
        println(s"  Label $label -> ${person.name}")
      }
    }
  }
}
