package com.kamilkurp.building

import scala.collection.mutable.ListBuffer

class Floor {
  var roomList: ListBuffer[Room] = _
  var doorList: ListBuffer[Door] = _
  var name: String = _

}


object Floor {
  def apply(name: String): Floor = {
    val floor = new Floor()

    floor.roomList = new ListBuffer[Room]()
    floor.doorList = new ListBuffer[Door]()

    floor.name = name

    floor
  }
}