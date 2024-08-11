package software.altitude.core.service

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.Altitude
import software.altitude.core.dao.FaceDao
import software.altitude.core.dao.PersonDao
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.Person
import software.altitude.core.transactions.TransactionManager
import software.altitude.core.util.Query
import software.altitude.core.util.QueryResult
import software.altitude.core.util.Sort
import software.altitude.core.util.SortDirection
import software.altitude.core.{Const => C}

object PersonService {
  val UNKNOWN_NAME_PREFIX = "Unknown"
}

class PersonService (val app: Altitude) extends BaseService[Person] {
  protected val dao: PersonDao = app.DAO.person
  private val faceDao: FaceDao = app.DAO.face

  override protected val txManager: TransactionManager = app.txManager

  override def add(objIn: Person, queryForDup: Option[Query] = None): JsObject = {
    throw new NotImplementedError("Use the alternate addPerson() method2")
  }

  def getPersonById(personId: String): Person = {
    txManager.asReadOnly[Person] {
      dao.getById(personId)
    }
  }

  def getFaceById(faceId: String): Face = {
    txManager.asReadOnly[Face] {
      faceDao.getById(faceId)
    }
  }

  def addFace(face: Face, asset: Asset, person: Person): Face = {
    require(person.persistedId.nonEmpty, "Cannot add a face to an unsaved person object")

    txManager.withTransaction[Face] {
      val persistedFace: Face = faceDao.add(face, asset, person)

      app.service.fileStore.addFace(persistedFace)

      // First time? Add the face to the person as the cover
      if (person.numOfFaces == 0) {
        setFaceAsCover(person, persistedFace)
      }

      // face number + 1
      increment(person.persistedId, C.Person.NUM_OF_FACES)

      val cachedPerson = app.service.faceCache.getPersonByLabel(persistedFace.personLabel.get)

      if (cachedPerson.isEmpty) {
        val personWithFace = person.copy(numOfFaces = 1)
        personWithFace.addFace(persistedFace)
        app.service.faceCache.putPerson(personWithFace)
      } else {
        app.service.faceCache.addFace(persistedFace)
      }

      app.service.faceRecognition.indexFace(persistedFace, person)

      persistedFace
    }
  }

  def addPerson(person: Person, asset: Option[Asset] = None): Person = {
    txManager.withTransaction[Person] {
      require(person.getFaces.size < 2, "Adding a new person with more than one face is currently not supported")

      // Without a face given, numOfFaces is 0, with a face given, it's 1
      val personForUpdate = person.copy(
        numOfFaces = person.getFaces.size,
      )

      val persistedPerson: Person = dao.add(personForUpdate)

      // if we are adding a person and it's the first face
      if (person.getFaces.size == 1) {
        val face = person.getFaces.head

        val persistedFace: Face = addFace(
          face,
          asset.getOrElse(throw new IllegalArgumentException("Asset ID is required")),
          persistedPerson)

        app.service.fileStore.addFace(persistedFace)

        // This face is the default cover
        setFaceAsCover(persistedPerson, persistedFace)

        persistedPerson.addFace(persistedFace)

        app.service.faceCache.putPerson(persistedPerson)

        return persistedPerson ++ Json.obj(
          C.Person.COVER_FACE_ID -> persistedFace.persistedId)
      }

      persistedPerson
    }
  }

  def merge(dest: Person, source: Person): Person = {
    if (source == dest) {
        throw new IllegalArgumentException("Cannot merge a person with itself. That's perverse!")
    }

    if (source.mergedIntoId.nonEmpty) {
      throw new IllegalArgumentException("Cannot merge a person that has already been merged")
    }

    if (dest.mergedWithIds.contains(source.persistedId)) {
      throw new IllegalArgumentException("Cannot merge a person that has already been merged with the destination")
    }

    logger.info(s"Merging person ${source.name} into ${dest.name}")

    txManager.withTransaction[Person] {

      if (source.mergedWithIds.nonEmpty) {
        /**
         * If the source person was merged with other people before, follow that relation and update THAT source with
         * current destination info.
         * This way, when of of the old merge sources is pulled by label from cache, it will point directly to this new
         * composite person.
         */
        logger.info(s"Source person ${source.name} was merged with other people before. IDs: ${source.mergedWithIds}")
        logger.info("Updating the old merge sources with the new destination info")
        val oldMergeSourcesQ = new Query().add(
          C.Person.ID -> Query.IN(source.mergedWithIds.toSet))

        updateByQuery(
          oldMergeSourcesQ,
          Map(
            C.Person.MERGED_INTO_ID -> dest.persistedId,
            C.Person.MERGED_INTO_LABEL -> dest.label))

        // update the cache with the new destination info
        source.mergedWithIds.foreach { oldMergeSourceId =>
          val oldMergeSource = getPersonById(oldMergeSourceId)
          app.service.faceCache.putPerson(oldMergeSource)
        }
      }

      // update the destination with the source person's id (it's a list of ids at the destination)
      val persistedDest: Person = dao.getById(dest.persistedId)
      val persistedSource: Person = dao.getById(source.persistedId)

      val mergedDest: Person = dao.updateMergedWithIds(dest, source.persistedId)
      val updatedDest = mergedDest.copy(numOfFaces = persistedDest.numOfFaces + persistedSource.numOfFaces)

      // update new destination count
      updateById(
        dest.persistedId,
        Map(C.Person.NUM_OF_FACES -> updatedDest.numOfFaces))

      // move all faces from source to destination
      logger.debug(s"Moving faces from ${source.name.get} to ${dest.name.get}")
      val q = new Query().add(C.Face.PERSON_ID -> source.persistedId)

      val allSourceFaces: List[Face] = faceDao.query(q).records.map(Face.fromJson(_))
      logger.info(s"Training the ${allSourceFaces.size} faces on the destination label ${dest.label}")
      // train the faces on the destination label
      allSourceFaces.foreach { face =>
          app.service.faceRecognition.indexFace(face, persistedDest)
      }

      faceDao.updateByQuery(q, Map(C.Face.PERSON_ID -> dest.persistedId))

      // specify ID/label of where the source person was merged into
      val updatedSource: Person = persistedSource.copy(
        mergedIntoId = Some(dest.persistedId),
        mergedIntoLabel = Some(dest.label))

      updatedSource.clearFaces()

      updateById(
        updatedSource.persistedId,
        Map(
          C.Person.MERGED_INTO_ID -> updatedSource.mergedIntoId.get,
          C.Person.MERGED_INTO_LABEL -> updatedSource.mergedIntoLabel.get,
          C.Person.NUM_OF_FACES -> 0
        ))

      // get the top face after merge
      val destFaces = getFaces(dest.persistedId, FaceRecognitionService.MAX_COMPARISONS_PER_PERSON)
      updatedDest.setFaces(destFaces)

      app.service.faceCache.putPerson(updatedSource)
      app.service.faceCache.putPerson(updatedDest)

      updatedDest
    }
  }

  def getFaces(personId: String, limit: Int = 50): List[Face] = {
    txManager.asReadOnly[List[Face]] {
      val sort: Sort = Sort(C.Face.DETECTION_SCORE, SortDirection.DESC)

      val q = new Query(
        params=Map(C.Face.PERSON_ID -> personId),
        sort=List(sort))

      val qRes: QueryResult = faceDao.query(q)
      qRes.records.take(limit).map(Face.fromJson(_))
    }
  }

  def setFaceAsCover(person: Person, face: Face): Person = {
    txManager.withTransaction[Person] {
      val personForUpdate = person.copy(coverFaceId = Some(face.persistedId))

      updateById(
        person.persistedId,
        Map(C.Person.COVER_FACE_ID -> face.persistedId))

      personForUpdate
    }
  }
}
