package software.altitude.core.service

import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java
import software.altitude.core.Altitude
import software.altitude.core.dao.FaceDao
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.Person
import software.altitude.core.{Const => C}

class FaceService(val app: Altitude) extends BaseService[Face] {
  Loader.load(classOf[opencv_java])

  override protected val dao: FaceDao = app.DAO.face
  def add(face: Face, asset: Asset, person: Person): Face = {
    val persistedFace: Face = dao.add(face, asset, person)

    app.service.fileStore.addFace(persistedFace)

    // First time? Add the face to the person as the cover
    if (person.numOfFaces == 0) {
      val personForUpdate = person.copy(coverFaceId = Some(persistedFace.persistedId))
      app.service.person.updateById(person.persistedId, personForUpdate, List(C.Person.COVER_FACE_ID))
    }

    // face number + 1
    app.service.person.increment(person.persistedId, C.Person.NUM_OF_FACES)

    persistedFace
  }
}
