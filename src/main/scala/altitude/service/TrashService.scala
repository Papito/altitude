package altitude.service

import altitude.Altitude
import altitude.dao.TrashDao
import altitude.models.Trash
import altitude.transactions.TransactionId
import org.slf4j.LoggerFactory
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.JsValue

class TrashService(app: Altitude) extends BaseService[Trash](app) {
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[TrashDao]

  def moveAsset(assetId: String)(implicit txId: TransactionId = new TransactionId): Trash = {
    val asset: JsValue = app.service.asset.getById(assetId)
    txManager.withTransaction[Trash] {
      // since the models are essentially the same, we can pull shit like this
      val trash = Trash.fromJson(asset)
      // delete the original asset
      app.service.asset.deleteById(assetId)
      // create the trash record
      DAO.add(trash)
    }
  }
}
