package software.altitude.core.service

import java.sql.SQLException
import play.api.libs.json.JsObject

import software.altitude.core.Altitude
import software.altitude.core.FieldConst
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
import software.altitude.core.util.Util.getDuplicateExceptionOrSame

object PersonService {
  val UNKNOWN_NAME_PREFIX = "Unknown"
}

class PersonService(val app: Altitude) extends BaseService[Person] {
  protected val dao: PersonDao = app.DAO.person
  private val faceDao: FaceDao = app.DAO.face

  override protected val txManager: TransactionManager = app.txManager

  override def add(objIn: Person): JsObject = {
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
    require(asset.persistedId.nonEmpty, "Cannot add a face to an unsaved asset object")

    txManager.withTransaction[Face] {

      /**
       * An image can have multiple faces, but can same person have multiple faces in the same image?
       *
       * Yep.
       *
       * In isolation, an image is very likely to not have such absurdity, but in the context of a larger data set, a previously
       * detected person may be detected again in the same image, given lax face detection/similarity thresholds.
       *
       * This is obviously wrong, but we can't really do anything about it. The user is responsible for properly maintaining
       * people and faces, and all we can do is silently fail and chug along.
       *
       * This is not ideal, and in the future we should mark the asset as "needs attention", or something.
       *
       * We shouldn't be ignoring the image as there is nothing really wrong with it.
       */
      var persistedFace: Option[Face] = None
      try {
        persistedFace = Some(faceDao.add(face, asset, person))
      } catch {
        case e: SQLException =>
          throw getDuplicateExceptionOrSame(
            e,
            Some(s"Face already exists for person ${person.label} in asset ${asset.persistedId}"))
        case ex: Exception =>
          throw ex
      }

      // First time? Add the face to the person as the cover
      if (person.numOfFaces == 0) {
        setFaceAsCover(person, persistedFace.get)
      }

      // face number + 1
      increment(person.persistedId, FieldConst.Person.NUM_OF_FACES)

      val cachedPerson = app.service.faceCache.getPersonByLabel(persistedFace.get.personLabel.get)

      if (cachedPerson.isEmpty) {
        val personWithFace = person.copy(numOfFaces = 1)
        personWithFace.addFace(persistedFace.get)
        app.service.faceCache.putPerson(personWithFace)
      } else {
        app.service.faceCache.addFace(persistedFace.get)
      }

      persistedFace.get
    }
  }

  def addPerson(person: Person, asset: Option[Asset] = None): Person = {
    txManager.withTransaction[Person] {
      require(person.getFaces.size < 2, "Adding a new person with more than one face is currently not supported")

      // Without a face given, numOfFaces is 0, with a face given, it's 1
      val personForUpdate = person.copy(
        numOfFaces = person.getFaces.size
      )

      dao.add(personForUpdate): Person
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
         * If the source person was merged with other people before, follow that relation and update THAT source with current
         * destination info. This way, when of the old merge sources is pulled by label from cache, it will point directly to this
         * new composite person.
         */
        logger.info(s"Source person ${source.name} was merged with other people before. IDs: ${source.mergedWithIds}")
        logger.info("Updating the old merge sources with the new destination info")
        val oldMergeSourcesQ = new Query().add(FieldConst.ID -> Query.IN(source.mergedWithIds.toSet))

        updateByQuery(
          oldMergeSourcesQ,
          Map(FieldConst.Person.MERGED_INTO_ID -> dest.persistedId, FieldConst.Person.MERGED_INTO_LABEL -> dest.label))

        // update the cache with the new destination info
        source.mergedWithIds.foreach {
          oldMergeSourceId =>
            val oldMergeSource = getPersonById(oldMergeSourceId)
            app.service.faceCache.putPerson(oldMergeSource)
        }
      }

      // update the destination with the source person's id (it's a list of IDs at the destination)
      val persistedDest: Person = dao.getById(dest.persistedId)
      val persistedSource: Person = dao.getById(source.persistedId)

      val mergedDest: Person = dao.updateMergedWithIds(dest, source.persistedId)
      val updatedDest = mergedDest.copy(numOfFaces = persistedDest.numOfFaces + persistedSource.numOfFaces)

      // update new destination count
      updateById(dest.persistedId, Map(FieldConst.Person.NUM_OF_FACES -> updatedDest.numOfFaces))

      // move all faces from source to destination
      logger.debug(s"Moving faces from ${source.name.get} to ${dest.name.get}")
      val q = new Query().add(FieldConst.Face.PERSON_ID -> source.persistedId)

      val allSourceFaces: List[Face] = faceDao.query(q).records.map(Face.fromJson(_))
      logger.info(s"Training the ${allSourceFaces.size} faces on the destination label ${dest.label}")

      /**
       * Train the faces on the destination label. We, however, do NOT have the training images stored in the database. We must
       * pull the binary data from the file store and create a copy of the face objects.
       */
      val allSourceFacesWithImageData = allSourceFaces.map {
        face =>
          val alignedGreyscaleData = app.service.fileStore.getAlignedGreyscaleFaceById(face.persistedId)
          face.copy(alignedImageGs = alignedGreyscaleData.data)
      }

      app.service.faceRecognition.addFacesToPerson(allSourceFacesWithImageData, persistedDest)

      faceDao.updateByQuery(q, Map(FieldConst.Face.PERSON_ID -> dest.persistedId))

      // specify ID/label of where the source person was merged into
      val updatedSource: Person = persistedSource.copy(mergedIntoId = Some(dest.persistedId), mergedIntoLabel = Some(dest.label))

      updatedSource.clearFaces()

      updateById(
        updatedSource.persistedId,
        Map(
          FieldConst.Person.MERGED_INTO_ID -> updatedSource.mergedIntoId.get,
          FieldConst.Person.MERGED_INTO_LABEL -> updatedSource.mergedIntoLabel.get,
          FieldConst.Person.NUM_OF_FACES -> 0
        )
      )

      // get the top face after merge
      val destFaces = getPersonFaces(dest.persistedId, FaceRecognitionService.MAX_COMPARISONS_PER_PERSON)
      updatedDest.setFaces(destFaces)

      // The source person is now empty but still has to be there to serve as a redirect to the new destination
      app.service.faceCache.putPerson(updatedSource)
      app.service.faceCache.putPerson(updatedDest)

      updatedDest
    }
  }

  def getPersonFaces(personId: String, limit: Int = 50): List[Face] = {
    txManager.asReadOnly[List[Face]] {
      val sort: Sort = Sort(FieldConst.Face.DETECTION_SCORE, SortDirection.DESC)

      val q = new Query(params = Map(FieldConst.Face.PERSON_ID -> personId), sort = List(sort))

      val qRes: QueryResult = faceDao.query(q)
      qRes.records.take(limit).map(Face.fromJson(_))
    }
  }

  def getAssetFaces(assetId: String): List[Face] = {
    txManager.asReadOnly[List[Face]] {
      val q = new Query(params = Map(FieldConst.Face.ASSET_ID -> assetId))

      val qRes: QueryResult = faceDao.query(q)
      qRes.records.map(Face.fromJson(_))
    }
  }

  def getPeople(assetId: String): List[Person] = {
    txManager.asReadOnly[List[Person]] {
      val faces = getAssetFaces(assetId)
      val personIds = faces.map(_.personId.get)

      val q = new Query(params = Map(FieldConst.ID -> Query.IN(personIds.toSet)))

      val qRes: QueryResult = dao.query(q)
      qRes.records.map(Person.fromJson(_))
    }
  }

  def setFaceAsCover(person: Person, face: Face): Person = {
    txManager.withTransaction {
      val personForUpdate = person.copy(coverFaceId = Some(face.persistedId))

      updateById(person.persistedId, Map(FieldConst.Person.COVER_FACE_ID -> face.persistedId))

      personForUpdate
    }
  }

  def updateName(person: Person, newName: String): Person = {
    txManager.withTransaction {

      updateById(person.persistedId, Map(FieldConst.Person.NAME -> newName))

      person.copy(name = Some(newName))
    }
  }

  def getAll: List[Person] = {
    txManager.asReadOnly {
      dao.getAll.values.toList
    }
  }
}
