package software.altitude.core.service

import org.apache.pekko.Done
import org.apache.pekko.stream.scaladsl.Source
import play.api.libs.json.JsObject
import software.altitude.core.Altitude
import software.altitude.core.FieldConst
import software.altitude.core.RequestContext
import software.altitude.core.dao.FaceDao
import software.altitude.core.dao.PersonDao
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.Person
import software.altitude.core.pipeline.PipelineTypes.PipelineContext
import software.altitude.core.transactions.TransactionManager
import software.altitude.core.util.Query
import software.altitude.core.util.QueryResult
import software.altitude.core.util.Sort
import software.altitude.core.util.SortDirection
import software.altitude.core.util.Util.getDuplicateExceptionOrSame

import java.sql.SQLException
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

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

      if (person.numOfFaces == 0) {
        setFaceAsCover(person, persistedFace.get)
      }

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

        logger.info(s"Source person ${source.name} was merged with other people before. IDs: ${source.mergedWithIds}")
        logger.info("Updating the old merge sources with the new destination info")
        val oldMergeSourcesQ = new Query().add(FieldConst.ID -> Query.IN(source.mergedWithIds.toSet))

        updateByQuery(
          oldMergeSourcesQ,
          Map(FieldConst.Person.MERGED_INTO_ID -> dest.persistedId, FieldConst.Person.MERGED_INTO_LABEL -> dest.label))

        source.mergedWithIds.foreach {
          oldMergeSourceId =>
            val oldMergeSource = getPersonById(oldMergeSourceId)
            app.service.faceCache.putPerson(oldMergeSource)
        }
      }

      val persistedDest: Person = dao.getById(dest.persistedId)
      val persistedSource: Person = dao.getById(source.persistedId)

      val mergedDest: Person = dao.updateMergedWithIds(dest, source.persistedId)
      val updatedDest = mergedDest.copy(numOfFaces = persistedDest.numOfFaces + persistedSource.numOfFaces)

      updateById(dest.persistedId, Map(FieldConst.Person.NUM_OF_FACES -> updatedDest.numOfFaces))

      logger.debug(s"Moving faces from ${source.name.get} to ${dest.name.get}")
      val query = new Query().add(FieldConst.Face.PERSON_ID -> source.persistedId)

      val allSourceFaces: List[Face] = faceDao.query(query).records.map(Face.fromJson(_))
      logger.info(s"Training the ${allSourceFaces.size} faces on the destination label ${dest.label}")

      val sourceFacesWithNewDestLabel: List[Face] = allSourceFaces.map(face => face.copy(personLabel = Some(dest.label)))

      /**
       * Take source faces, update them with destination ML label, and push via stream to the training pipeline
       */
      val pipelineContext = PipelineContext(RequestContext.getRepository, null)
      val trainingPipelineSource = Source.fromIterator(() => sourceFacesWithNewDestLabel.iterator).map((_, pipelineContext))
      val pipelineResFuture: Future[Done] = app.service.bulkFaceRecTrainingPipelineService.run(trainingPipelineSource)
      Await.result(pipelineResFuture, Duration.Inf)

      // faces from source are moved to the new person and ML model label
      faceDao.updateByQuery(query, Map(
        FieldConst.Face.PERSON_ID -> dest.persistedId,
        FieldConst.Face.PERSON_LABEL -> dest.label)
      )

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

      val destFaces = getPersonFaces(dest.persistedId, FaceRecognitionService.MAX_COMPARISONS_PER_PERSON)
      updatedDest.setFaces(destFaces)

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

      updateById(person.persistedId, Map(
        FieldConst.Person.NAME -> newName,
        FieldConst.Person.NAME_FOR_SORT -> newName.toLowerCase()
      ))

      person.copy(name = Some(newName))
    }
  }

  def getAll: List[Person] = {
    txManager.asReadOnly {
      dao.getAll.values.toList
    }
  }

  def getAllAboveThreshold: List[Person] = {
    txManager.asReadOnly {
      dao.getAllAboveThreshold
    }
  }
}
