package software.altitude.core.service

import research.Person
import software.altitude.core.Altitude

import scala.collection.mutable


class FaceCacheService(app: Altitude) {
  private type Label = Int
  private type PersonCache = mutable.Map[Label, Person]
  private type RepositoryPersonCache = mutable.Map[String, PersonCache]

  private def newPersonCache(): PersonCache = mutable.Map[Label, Person]()

  // Model label (int) -> Person
  private val repoPersonCache: PersonCache = newPersonCache()

  // Repo ID -> Person Cache
  private val cache: RepositoryPersonCache = mutable.Map[String, PersonCache]()

  private def getRepositoryPersonCache(repository: String): PersonCache = {
    cache.getOrElseUpdate(repository, newPersonCache())
  }

  def getPerson(repositoryId: String, id: Label): Option[Person] = {
    getRepositoryPersonCache(repositoryId).get(id)
  }

  def putPerson(repositoryId: String, id: Label, person: Person): Unit = {
    getRepositoryPersonCache(repositoryId).put(id, person)
  }

  def removePerson(repositoryId: String, id: Label): Unit = {
    getRepositoryPersonCache(repositoryId).remove(id)
  }
}
