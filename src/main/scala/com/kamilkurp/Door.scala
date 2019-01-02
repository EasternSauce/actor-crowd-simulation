package com.kamilkurp

class Door(val name: String, var room: Room, var x: Float, var y: Float) extends Entity{
  override var w = 40.0f
  override var h = 70.0f
  override var currentVelocityX = 0.0f
  override var currentVelocityY = 0.0f
  var leadingToDoor: Door = _

  room.doorList += this

  override def changeRoom(room: Room, newX: Float, newY: Float): Unit = {
    //do nothing
  }

  def connectWith(door: Door): Unit = {
    leadingToDoor = door
    door.leadingToDoor = this
  }

  override def onCollision(entity: Entity): Unit = {
    var foundSpot: (Float, Float) = null
    for (gridX <- Seq(-10-entity.w,(w-entity.w)/w,w+10)){
      for (gridY <- Seq(-10-entity.h,(h-entity.h)/h,h+10)){
        val potentialSpotX = leadingToDoor.x + gridX
        val potentialSpotY = leadingToDoor.y + gridY
        if (!Globals.isRectOccupied(leadingToDoor.room, potentialSpotX, potentialSpotY, entity.w, entity.h)) {
          foundSpot = (potentialSpotX, potentialSpotY)
        }
      }
    }
    if( foundSpot != null) {
      println("found a spot on " + foundSpot._1 + " " + foundSpot._2)
      println("leading door x" + leadingToDoor.x + " y " + leadingToDoor.y)

      entity.changeRoom(leadingToDoor.room, foundSpot._1, foundSpot._2)
      println("x: "+ entity.x + " y: " + entity.y)
    }
  }


}
