package com.kamilkurp.entities

import com.kamilkurp.{Globals, Room}
import org.newdawn.slick.geom.{Rectangle, Shape}
import org.newdawn.slick.{Graphics, Image}

class Door(val name: String, var room: Room, var posX: Float, var posY: Float, var image: Image) extends Entity {
  override var currentVelocityX = 0.0f
  override var currentVelocityY = 0.0f
  override var shape: Shape = _
  var leadingToDoor: Door = _

  room.doorList += this

  shape = new Rectangle(posX, posY,24,48)

  override def changeRoom(room: Room, newX: Float, newY: Float): Unit = {
    //do nothing
  }

  def connectWith(door: Door): Unit = {
    leadingToDoor = door
    door.leadingToDoor = this
  }

  override def onCollision(entity: Entity): Unit = {
    var foundSpot: (Float, Float) = null
    for (gridX <- Seq(-10 - entity.shape.getWidth, (shape.getWidth - entity.shape.getWidth) / shape.getWidth, shape.getWidth + 10)) {
      for (gridY <- Seq(-10 - entity.shape.getHeight, (shape.getHeight - entity.shape.getHeight) / shape.getHeight, shape.getHeight + 10)) {
        val potentialSpotX = leadingToDoor.posX + gridX
        val potentialSpotY = leadingToDoor.posY + gridY
        if (!Globals.isRectOccupied(leadingToDoor.room, potentialSpotX, potentialSpotY, entity.shape.getWidth, entity.shape.getHeight)) {
          foundSpot = (potentialSpotX, potentialSpotY)
        }
      }
    }
    if (foundSpot != null) {
      entity.changeRoom(leadingToDoor.room, foundSpot._1, foundSpot._2)
    }
  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.drawImage(this.image, room.x + shape.getX - offsetX, room.y + shape.getY - offsetY)

  }

}
