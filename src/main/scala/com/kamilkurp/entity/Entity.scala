package com.kamilkurp.entity

import com.kamilkurp.building.{Door, Room}
import org.newdawn.slick.Image
import org.newdawn.slick.geom.Shape

abstract class Entity() {
  var name: String
  var currentVelocityX: Float
  var currentVelocityY: Float
  var room: Room
  var image: Image

  var shape: Shape

  var debug: Boolean

  def onCollision(entity: Entity)

  def changeRoom(entryDoor: Door, newX: Float, newY: Float)

  def getDistanceTo(entity: Entity): Float = {
    val differenceSquaredX = Math.pow(entity.shape.getCenterX.doubleValue() - shape.getCenterX.doubleValue(), 2)
    val differenceSquaredY = Math.pow(entity.shape.getCenterY.doubleValue() - shape.getCenterY.doubleValue(), 2)
    Math.sqrt(differenceSquaredX + differenceSquaredY).floatValue()
  }

  def getDistanceTo(x: Float, y: Float): Float = {
    val differenceSquaredX = Math.pow(x.doubleValue() - shape.getCenterX.doubleValue(), 2)
    val differenceSquaredY = Math.pow(y.doubleValue() - shape.getCenterY.doubleValue(), 2)
    Math.sqrt(differenceSquaredX + differenceSquaredY).floatValue()
  }
}
