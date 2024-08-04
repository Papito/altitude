package software.altitude.core.service

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
        val personForUpdate = person.copy(coverFaceId = Some(persistedFace.persistedId))
        updateById(person.persistedId, personForUpdate, List(C.Person.COVER_FACE_ID))
      }

      // face number + 1
      increment(person.persistedId, C.Person.NUM_OF_FACES)

      persistedFace
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
}
