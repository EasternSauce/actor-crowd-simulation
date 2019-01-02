package com.kamilkurp

abstract class Entity() {
  var x: Float
  var y: Float
  var w: Float
  var h: Float
  var currentVelocityX: Float
  var currentVelocityY: Float
  val name: String
  var room: Room
  def onCollision(entity: Entity)
  def changeRoom(room: Room, newX: Float, newY: Float)
}
