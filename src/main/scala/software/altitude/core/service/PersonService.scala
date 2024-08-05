package software.altitude.core.service

import play.api.libs.json.Json
import software.altitude.core.Altitude
import software.altitude.core.dao.FaceDao
import software.altitude.core.dao.PersonDao
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.Person
import software.altitude.core.transactions.TransactionManager
import software.altitude.core.{Const => C}

object PersonService {
  val UNKNOWN_NAME_PREFIX = "Unknown"
}

class PersonService (val app: Altitude) extends BaseService[Person] {
  protected val dao: PersonDao = app.DAO.person
  private val faceDao: FaceDao = app.DAO.face

  override protected val txManager: TransactionManager = app.txManager

  def getFaceById(faceId: String): Face = {
    txManager.asReadOnly[Face] {
      faceDao.getById(faceId)
    }
  }

  def addFace(face: Face, asset: Asset, person: Person): Face = {
    txManager.withTransaction[Face] {
      val persistedFace: Face = faceDao.add(face, asset, person)

      app.service.fileStore.addFace(persistedFace)

      // First time? Add the face to the person as the cover
      if (person.numOfFaces == 0) {
        setFaceAsCover(person, persistedFace)
      }

      // face number + 1
      increment(person.persistedId, C.Person.NUM_OF_FACES)

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

      // if we are adding a person and have a face to show for it
      if (person.getFaces.size == 1) {
        val face = person.getFaces.head

        val persistedFace: Face = faceDao.add(
          face,
          asset.getOrElse(throw new IllegalArgumentException("Asset ID is required")),
          persistedPerson)

        app.service.fileStore.addFace(persistedFace)

        // This face is the default cover
        setFaceAsCover(persistedPerson, persistedFace)

        persistedPerson.addFace(persistedFace)
        persistedPerson ++ Json.obj(C.Person.COVER_FACE_ID -> persistedFace.persistedId)
      }

      persistedPerson
    }
  }

  def merge(dest: Person, source: Person): Person = {
    txManager.withTransaction[Person] {
      // update the destination with the source person's id (it's a list of ids at the destination)
      val mergedPerson: Person = dao.updateMergedWithIds(dest, source.persistedId)

      // update the source person with the destination person's id
      val updatedSource = source.copy(
        mergedIntoId = Some(mergedPerson.persistedId),
      )

      updateById(updatedSource.persistedId, updatedSource, List(C.Person.MERGED_INTO_ID))

      mergedPerson
    }
  }

  def setFaceAsCover(person: Person, face: Face): Person = {
    txManager.withTransaction[Person] {
      val personForUpdate = person.copy(coverFaceId = Some(face.persistedId))
      updateById(person.persistedId, personForUpdate, List(C.Person.COVER_FACE_ID))
      personForUpdate
    }
  }
}
