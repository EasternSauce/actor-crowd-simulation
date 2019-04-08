package com.kamilkurp.util

import com.kamilkurp.building.Room
import com.kamilkurp.entity.Entity

object Globals {
  val AGENT_SIZE: Int = 40
  val WINDOW_X: Int = 2560
  val WINDOW_Y: Int = 1440
  val SCALE_X: Float = 0.5f
  val SCALE_Y: Float = 0.5f

  def manageCollisions(room: Room, entity: Entity): CollisionDetails = {

    val collisionDetails: CollisionDetails = new CollisionDetails(false, false)

    if (entity.shape.getX + entity.currentVelocityX < 0 || entity.shape.getX + entity.currentVelocityX > room.w - entity.shape.getWidth) collisionDetails.colX = true
    if (entity.shape.getY + entity.currentVelocityY < 0 || entity.shape.getY + entity.currentVelocityY > room.h - entity.shape.getHeight) collisionDetails.colY = true

    room.agentList.filter(agent => agent != entity).foreach(agent => {

      var collided = false

      if (intersectsX(entity, agent.shape.getX, agent.shape.getY, agent.shape.getWidth, agent.shape.getHeight)) {
        collisionDetails.colX = true
        collided = true
      }
      if (intersectsY(entity, agent.shape.getX, agent.shape.getY, agent.shape.getWidth, agent.shape.getHeight)) {
        collisionDetails.colY = true
        collided = true
      }

      if (collided) {
        entity.onCollision(agent)
        agent.onCollision(entity)
      }
    })

    room.meetPointList.foreach(meetPoint => {

      var collided = false

      if (intersectsX(entity, meetPoint.shape.getX, meetPoint.shape.getY, meetPoint.shape.getWidth, meetPoint.shape.getHeight)) {
        collisionDetails.colX = true
        collided = true
      }
      if (intersectsY(entity, meetPoint.shape.getX, meetPoint.shape.getY, meetPoint.shape.getWidth, meetPoint.shape.getHeight)) {
        collisionDetails.colY = true
        collided = true
      }

      if (collided) {
        entity.onCollision(meetPoint)
        meetPoint.onCollision(entity)
      }
    })

    room.doorList.foreach(door => {
      if (intersects(entity, door.shape.getX, door.shape.getY, door.shape.getWidth, door.shape.getHeight)) {
        entity.onCollision(door)
        door.onCollision(entity)
      }
    })

    collisionDetails
  }

  def isRectOccupied(room: Room, x: Float, y: Float, w: Float, h: Float): Boolean = {
    var occupied = false

    if (x < 0 || x > room.w - w) occupied = true
    if (y < 0 || y > room.h - h) occupied = true

    room.agentList.foreach(agent => {
      if (intersects(agent, x, y, w, h)) {
        occupied = true
      }
    })

    room.doorList.foreach(door => {
      if (intersects(door, x, y, w, h)) {
        occupied = true
      }
    })

    occupied
  }

  private def intersects(entity: Entity, thatX: Float, thatY: Float, thatW: Float, thatH: Float): Boolean = {

    if (intersectsX(entity, thatX, thatY, thatW, thatH) || intersectsY(entity, thatX, thatY, thatW, thatH)) true
    else false

  }

  private def intersectsX(entity: Entity, thatX: Float, thatY: Float, thatW: Float, thatH: Float): Boolean = {

    if (entity.shape.getX + entity.currentVelocityX < thatX + thatW &&
      entity.shape.getX + entity.currentVelocityX + entity.shape.getWidth > thatX &&
      entity.shape.getY < thatY + thatH &&
      entity.shape.getHeight + entity.shape.getY > thatY) true
    else false

  }

  private def intersectsY(entity: Entity, thatX: Float, thatY: Float, thatW: Float, thatH: Float): Boolean = {

    if (entity.shape.getX < thatX + thatW &&
      entity.shape.getX + entity.shape.getWidth > thatX &&
      entity.shape.getY + entity.currentVelocityY < thatY + thatH &&
      entity.shape.getHeight + entity.shape.getY + entity.currentVelocityY > thatY) true
    else false

  }

  class CollisionDetails(var colX: Boolean, var colY: Boolean)

}
