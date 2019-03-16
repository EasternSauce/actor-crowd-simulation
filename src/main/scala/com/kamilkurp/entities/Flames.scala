package com.kamilkurp.entities

import com.kamilkurp.Room
import org.newdawn.slick.Image
import org.newdawn.slick.geom.{Rectangle, Shape}

class Flames extends Entity {
  override var currentVelocityX: Float = 0
  override var currentVelocityY: Float = 0
  override var shape: Shape = _
  override val name: String = "fire"
  override var room: Room = _
  override var image: Image = _

  override def onCollision(entity: Entity): Unit = {

  }

  override def changeRoom(room: Room, newX: Float, newY: Float): Unit = {

  }

}
