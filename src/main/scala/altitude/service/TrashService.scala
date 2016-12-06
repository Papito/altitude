package altitude.service

import altitude.Altitude
import altitude.dao.TrashDao
import altitude.models.{Trash, User}
import altitude.transactions.TransactionId
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

class TrashService(app: Altitude) extends BaseService[Trash](app) {
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[TrashDao]

  def recycleAsset(assetId: String)(implicit user: User, txId: TransactionId = new TransactionId): Trash = {
    val asset: JsValue = app.service.asset.getById(assetId)
    txManager.withTransaction[Trash] {
         // delete the original asset
      app.service.library.deleteById(assetId)

      // since the models are essentially the same, we can pull shit like this
      val trashed: Trash = Trash.fromJson(asset)

      // create the trash record
      DAO.add(trashed)
    }
  }

  def recycleAssets(assetIds: Set[String])(implicit user: User, txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      assetIds.foreach(recycleAsset)
    }
  }
}
