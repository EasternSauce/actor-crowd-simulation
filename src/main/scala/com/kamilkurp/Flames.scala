package com.kamilkurp
import org.newdawn.slick.Image

class Flames extends Entity {
  override val name: String = "fire"
  override var x: Float = _
  override var y: Float = _
  override var w: Float = _
  override var h: Float = _
  override var currentVelocityX: Float = 0
  override var currentVelocityY: Float = 0
  override var room: Room = _
  override var image: Image = _

  override def onCollision(entity: Entity): Unit = {

  }

  override def changeRoom(room: Room, newX: Float, newY: Float): Unit = {

  }

}
