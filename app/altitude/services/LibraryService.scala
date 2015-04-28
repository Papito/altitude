package altitude.services

import java.util.NoSuchElementException

import altitude.Util.log
import altitude.dao.{TransactionId, LibraryDao}
import altitude.exceptions.DuplicateException
import altitude.models.Asset
import altitude.models.search.Query
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.JsObject
import altitude.{Const => C}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LibraryService extends BaseService[Asset] {
  override protected val DAO = app.injector.instance[LibraryDao]

  override def add(asset: Asset)(implicit txId: TransactionId = new TransactionId): Future[JsObject] = {
    txManager.withTransaction[Future[JsObject]] {
      val existing = DAO.query(Query(Map(C.Asset.MD5 -> asset.md5)))

      val f = for {
        duplicates <- existing
        res <- super.add(asset)
        if duplicates.length == 0
      } yield res

      f recover {
        // if filter fails and there IS a duplicate
        case ex: NoSuchElementException => {
          log.warn(s"Asset already exists for ${asset.path}")
          throw new DuplicateException(s"Duplicate for ${asset.path}")
        }
      }
    }
  }
}
