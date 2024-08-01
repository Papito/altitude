package software.altitude.core.service

import software.altitude.core.Altitude
import software.altitude.core.dao.PersonDao
import software.altitude.core.models.Person
import software.altitude.core.transactions.TransactionManager

class PersonService (val app: Altitude) extends BaseService[Person] {
  protected val dao: PersonDao = app.DAO.person

  override protected val txManager: TransactionManager = app.txManager
}
