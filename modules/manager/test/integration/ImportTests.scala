package integration

import org.scalatest._
import org.scalatestplus.play._

class ImportTests extends FunSuite with OneAppPerSuite {

  test("Import files") {
    val incomingPath = getClass.getResource("../files/incoming").getPath
    global.ManagerGlobal.importService.importAssets(path = incomingPath)
  }
}
