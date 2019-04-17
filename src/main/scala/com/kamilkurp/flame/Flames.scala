package com.kamilkurp.flame

import com.kamilkurp.building.{Door, Room}
import com.kamilkurp.entity.Entity
import org.newdawn.slick.geom.{Rectangle, Shape}
import org.newdawn.slick.{Graphics, Image}

class Flames(var room: Room, var posX: Float, var posY: Float, var image: Image) extends Entity {

  override var name: String = _
  override var currentVelocityX: Float = _
  override var currentVelocityY: Float = _
  override var shape: Shape = _
  var dontUpdate: Boolean = _

  name = "fire"
  currentVelocityX = 0
  currentVelocityY = 0
  dontUpdate = false
  shape = new Rectangle(posX, posY, image.getWidth, image.getHeight)

  override def onCollision(entity: Entity): Unit = {

  }

  override def changeRoom(entryDoor: Door, newX: Float, newY: Float): Unit = {

  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.drawImage(this.image, room.x + shape.getX - offsetX, room.y + shape.getY - offsetY)
  }




}
