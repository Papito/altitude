package software.altitude.core.dao.sqlite

import com.typesafe.config.Config
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.RequestContext
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Asset
import software.altitude.core.models.Face
import software.altitude.core.models.Person
import software.altitude.core.{Const => C}

import java.sql.PreparedStatement

class FaceDao(override val config: Config)
  extends BaseDao
    with software.altitude.core.dao.FaceDao
    with SqliteOverrides {

  override final val tableName = "face"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val embeddingsArray = getFloatListFromJsonStr(rec(C.Face.EMBEDDINGS).asInstanceOf[String], C.Face.EMBEDDINGS)
    val featuresArray = getFloatListFromJsonStr(rec(C.Face.FEATURES).asInstanceOf[String], C.Face.FEATURES)

    val model = Face(
      id = Some(rec(C.Base.ID).asInstanceOf[String]),
      x1 = rec(C.Face.X1).asInstanceOf[Int],
      y1 = rec(C.Face.Y1).asInstanceOf[Int],
      width = rec(C.Face.WIDTH).asInstanceOf[Int],
      height = rec(C.Face.HEIGHT).asInstanceOf[Int],
      assetId = Some(rec(C.Face.ASSET_ID).asInstanceOf[String]),
      personId = Some(rec(C.Face.PERSON_ID).asInstanceOf[String]),
      detectionScore = rec(C.Face.DETECTION_SCORE).asInstanceOf[Double],
      embeddings = embeddingsArray.toArray,
      features = featuresArray.toArray,
      image = rec(C.Face.IMAGE).asInstanceOf[Array[Byte]],
      aligned_image = rec(C.Face.ALIGNED_IMAGE).asInstanceOf[Array[Byte]],
      aligned_image_gs = rec(C.Face.ALIGNED_IMAGE_GS).asInstanceOf[Array[Byte]]
    )

    model
  }

  override def add(jsonIn: JsObject, asset: Asset, person: Person): JsObject = {
    val face: Face = jsonIn: Face

    val id = BaseDao.genId

    val sql = s"""
        INSERT INTO $tableName (${C.Face.ID}, ${C.Base.REPO_ID}, ${C.Face.X1}, ${C.Face.Y1}, ${C.Face.WIDTH}, ${C.Face.HEIGHT},
                                ${C.Face.ASSET_ID}, ${C.Face.PERSON_ID}, ${C.Face.DETECTION_SCORE}, ${C.Face.EMBEDDINGS},
                                ${C.Face.FEATURES}, ${C.Face.IMAGE}, ${C.Face.ALIGNED_IMAGE}, ${C.Face.ALIGNED_IMAGE_GS})
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """

    val conn = RequestContext.getConn

    val embeddingsArrayJson = Json.obj(
      C.Face.EMBEDDINGS -> Json.toJson(face.embeddings),
    )

    val featuresArrayJson = Json.obj(
      C.Face.FEATURES -> Json.toJson(face.features),
    )

    val preparedStatement: PreparedStatement = conn.prepareStatement(sql)
    preparedStatement.setString(1, id)
    preparedStatement.setString(2, RequestContext.getRepository.persistedId)
    preparedStatement.setInt(3, face.x1)
    preparedStatement.setInt(4, face.y1)
    preparedStatement.setInt(5, face.width)
    preparedStatement.setInt(6, face.height)
    preparedStatement.setString(7, asset.persistedId)
    preparedStatement.setString(8, person.persistedId)
    preparedStatement.setDouble(9, face.detectionScore)
    preparedStatement.setString(10, embeddingsArrayJson.toString())
    preparedStatement.setString(11, featuresArrayJson.toString())
    preparedStatement.setBytes(12, face.image)
    preparedStatement.setBytes(13, face.aligned_image)
    preparedStatement.setBytes(14, face.aligned_image_gs)
    preparedStatement.execute()

    jsonIn ++ Json.obj(
      C.Base.ID -> id,
      C.Face.ASSET_ID -> asset.id.get,
      C.Face.PERSON_ID -> person.id.get
    )
  }
}
