package software.altitude.core.service

import software.altitude.core.Altitude
import software.altitude.core.dao.PersonDao
import software.altitude.core.models.Person
import software.altitude.core.transactions.TransactionManager
import software.altitude.core.{Const => C}

class PersonService (val app: Altitude) extends BaseService[Person] {
  protected val dao: PersonDao = app.DAO.person

  override protected val txManager: TransactionManager = app.txManager

  def merge(dest: Person, source: Person): Person = {
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
