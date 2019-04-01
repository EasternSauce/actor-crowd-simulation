package com.kamilkurp.entities

import com.kamilkurp.Room
import org.newdawn.slick.geom.{Rectangle, Shape, Transform}
import org.newdawn.slick.{Color, Graphics, Image}

class MeetPoint(val name: String, var room: Room, var posX: Float, var posY: Float) extends Entity {

  override var currentVelocityX: Float = 0
  override var currentVelocityY: Float = 0
  override var image: Image = _
  override var shape: Shape = _
//  override var allowChangeRoom: Boolean = false

  shape = new Rectangle(posX, posY, 30, 30)

  override def onCollision(entity: Entity): Unit = {

  }

  override def changeRoom(entryDoor: Door, newX: Float, newY: Float): Unit = {

  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    val t: Transform = Transform.createTranslateTransform(room.x - offsetX, room.y - offsetY)
    g.setColor(Color.green)
    g.fill(shape.transform(t))
  }

}
