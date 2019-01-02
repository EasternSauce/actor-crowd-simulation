package com.kamilkurp

object Globals {
  val CHARACTER_SIZE: Int = 40
  val WINDOW_X: Int = 1440
  val WINDOW_Y: Int = 900

  private def intersects(entity: Entity, thatX: Float, thatY: Float, thatW: Float, thatH: Float): Boolean = {

    if (entity.x + entity.currentVelocityX < thatX + thatW &&
      entity.x + entity.currentVelocityX + entity.w > thatX &&
      entity.y + entity.currentVelocityY < thatY + thatH &&
      entity.h + entity.y + entity.currentVelocityY > thatY) true
    else false

  }

  def isColliding(room: Room, entity: Entity): Boolean = {
    var collided = false

    if (entity.x + entity.currentVelocityX < 0 || entity.x + entity.currentVelocityX > room.w - entity.w) collided = true
    if (entity.y + entity.currentVelocityY < 0 || entity.y + entity.currentVelocityY > room.h - entity.h) collided = true

    room.characterList.filter(character => character != entity).foreach(character => {
      if (intersects(entity, character.x, character.y, character.w, character.h)) {
        collided = true
        entity.onCollision(character)
        character.onCollision(entity)
      }
    })

    room.doorList.foreach(door => {
      if (intersects(entity, door.x, door.y, door.w, door.h)) {
        //println("door collision! x: " + door.x + " y: " + door.y + " w: " + door.w + " h: " + door.h + " collided with x: " + entity.x + " y: " + entity.y + " w: " + entity.w + " h: " + entity.h )
        entity.onCollision(door)
        door.onCollision(entity)
      }
    })


    collided
  }

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