package com.kamilkurp.building

import com.kamilkurp.entity.Entity
import com.kamilkurp.util.Globals
import org.newdawn.slick.geom.{Rectangle, Shape, Vector2f}
import org.newdawn.slick.{Color, Graphics, Image}

class Door(var name: String, var room: Room, var posX: Float, var posY: Float, var image: Image) extends Entity {
  override var shape: Shape = _
  override var debug: Boolean = false

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


  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.drawImage(this.image, room.x + shape.getX - offsetX, room.y + shape.getY - offsetY)
    g.setColor(Color.green)
    g.drawString(this.name, room.x + shape.getX - offsetX, room.y + shape.getY - offsetY)

  }

}
