package altitude.service

import altitude.Altitude
import altitude.dao.FolderDao
import altitude.models.Folder
import altitude.transactions.TransactionId

import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.JsValue
import altitude.{Const => C}


class FolderService(app: Altitude) extends BaseService[Folder](app){
  override protected val DAO = app.injector.instance[FolderDao]

  def getHierarchy(rootId: Option[String] = None)(implicit txId: TransactionId) = findChildren(DAO.getAll())

  private def findChildren(all: List[JsValue], parentId: Option[String] = None): List[Folder] = {
    val children = all.filter(j => (j \ C.Folder.PARENT_ID).asOpt[String] == parentId)

    for (folder <- children) yield  {
      val id: Option[String] = (folder \ C.Folder.ID).asOpt[String]
      val name =  (folder \ C.Folder.NAME).as[String]

      Folder(id = id, name = name,
        children = findChildren(all, id))
    }
  }
}
