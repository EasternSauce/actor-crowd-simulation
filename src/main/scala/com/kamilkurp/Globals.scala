package com.kamilkurp

import com.kamilkurp.entities.Entity

object Globals {
  val CHARACTER_SIZE: Int = 40
  val WINDOW_X: Int = 2560
  val WINDOW_Y: Int = 1440
  val SCALE_X: Float = 2.0f
  val SCALE_Y: Float = 2.0f

  private def intersectsX(entity: Entity, thatX: Float, thatY: Float, thatW: Float, thatH: Float): Boolean = {

    if (entity.x + entity.currentVelocityX < thatX + thatW &&
      entity.x + entity.currentVelocityX + entity.w > thatX &&
      entity.y < thatY + thatH &&
      entity.h + entity.y > thatY) true
    else false

  }

  private def intersectsY(entity: Entity, thatX: Float, thatY: Float, thatW: Float, thatH: Float): Boolean = {

    if (entity.x < thatX + thatW &&
      entity.x + entity.w > thatX &&
      entity.y + entity.currentVelocityY < thatY + thatH &&
      entity.h + entity.y + entity.currentVelocityY > thatY) true
    else false

  }

  private def intersects(entity: Entity, thatX: Float, thatY: Float, thatW: Float, thatH: Float): Boolean = {

    if (intersectsX(entity, thatX, thatY, thatW, thatH) || intersectsY(entity, thatX, thatY, thatW, thatH)) true
    else false

  }

  def manageCollisions(room: Room, entity: Entity): CollisionDetails = {
    val collisionDetails: CollisionDetails = new CollisionDetails(false, false)

    if (entity.x + entity.currentVelocityX < 0 || entity.x + entity.currentVelocityX > room.w - entity.w) collisionDetails.colX = true
    if (entity.y + entity.currentVelocityY < 0 || entity.y + entity.currentVelocityY > room.h - entity.h) collisionDetails.colY = true

    room.characterList.filter(character => character != entity).foreach(character => {
      var collided = false

      if (intersectsX(entity, character.x, character.y, character.w, character.h)) {
        collisionDetails.colX = true
        collided = true
      }
      if (intersectsY(entity, character.x, character.y, character.w, character.h)) {
        collisionDetails.colY = true
        collided = true
      }

      if (collided) {
        entity.onCollision(character)
        character.onCollision(entity)
      }
    })

    room.doorList.foreach(door => {
      if (intersects(entity, door.x, door.y, door.w, door.h)) {
        entity.onCollision(door)
        door.onCollision(entity)
      }
    })

    collisionDetails
  }

  class CollisionDetails(var colX: Boolean, var colY: Boolean)

  def isRectOccupied(room: Room, x: Float, y: Float, w: Float, h: Float): Boolean = {
    var occupied = false

    if (x < 0 || x > room.w - w) occupied = true
    if (y < 0 || y > room.h - h) occupied = true

    room.characterList.foreach(character => {
      if (intersects(character, x, y, w, h)) {
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

}
