package com.kamilkurp.entities

import com.kamilkurp.{Globals, Room}
import org.newdawn.slick.geom.{Rectangle, Shape, Vector2f}
import org.newdawn.slick.{Graphics, Image}

class Door(val name: String, var room: Room, var posX: Float, var posY: Float, var image: Image) extends Entity {
  override var currentVelocityX = 0.0f
  override var currentVelocityY = 0.0f
  override var shape: Shape = _
  override var allowChangeRoom: Boolean = false

  var leadingToDoor: Door = _

  room.doorList += this

  shape = new Rectangle(posX, posY, 24, 48)

  override def changeRoom(entryDoor: Door, newX: Float, newY: Float): Unit = {
    //do nothing
  }

  def connectWith(door: Door): Unit = {
    leadingToDoor = door
    door.leadingToDoor = this
  }

  override def onCollision(entity: Entity): Unit = {
    val normalVector = new Vector2f(entity.currentVelocityX, entity.currentVelocityY)
    normalVector.normalise()


    for (_ <- 1 to 36) {
      normalVector.setTheta(normalVector.getTheta + 10)
      val spotX = leadingToDoor.posX + normalVector.x * 100
      val spotY = leadingToDoor.posY + normalVector.y * 100

      if (!Globals.isRectOccupied(leadingToDoor.room, spotX, spotY, entity.shape.getWidth, entity.shape.getHeight)) {
        entity.changeRoom(this, spotX, spotY)
        return
      }
    }

  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.drawImage(this.image, room.x + shape.getX - offsetX, room.y + shape.getY - offsetY)

  }

}
