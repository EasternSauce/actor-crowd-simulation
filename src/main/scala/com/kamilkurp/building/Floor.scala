package com.kamilkurp.building

import scala.collection.mutable.ListBuffer

class Floor {
  var roomList: ListBuffer[Room] = _

}


object Floor {
  def apply(): Floor = {
    val floor = new Floor()

    floor.roomList = new ListBuffer[Room]()

    floor
  }
}