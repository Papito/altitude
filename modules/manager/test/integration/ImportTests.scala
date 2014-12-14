package integration

import org.scalatest._
import org.scalatestplus.play._
import org.scalatest.Matchers._

class ImportTests extends FunSuite with OneAppPerSuite {

  test("Import files") {
    val incomingPath = getClass.getResource("../files/incoming").getPath
    val assets = global.ManagerGlobal.importService.iterateAssets(path=incomingPath)
    assets should not be empty
  }
}
