package com.kamilkurp.behaviors

import com.kamilkurp.entities.Character
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class FollowingBehavior(character: Character) extends Behavior(character) {

  var deviationX: Float = 0
  var deviationY: Float = 0

  override var timerTimeout: Int = 3500

  var deviationTimer: Int = 0
  val deviationTimerTimeout: Int = 500

  var followX: Float = 0
  var followY: Float = 0

  def perform(delta: Int): Unit = {
    timer += delta
    deviationTimer += delta

    if (character.room.evacuationDoor == null || timer > timerTimeout) {
      character.behaviorSet -= "following"
      return
    }


    if (deviationTimer > deviationTimerTimeout) {
      deviationX = 0.3f * Random.nextFloat() - 0.15f
      deviationY = 0.3f * Random.nextFloat() - 0.15f
      deviationTimer = 0
    }

    val normalVector = new Vector2f(followX - character.x, followY - character.y)
    normalVector.normalise()

    character.walkAngle = normalVector.getTheta.floatValue()

    character.currentVelocityX = (normalVector.x + deviationX) * character.speed * (1f - character.slow)
    character.currentVelocityY = (normalVector.y + deviationY) * character.speed * (1f - character.slow)
  }

  def start(x: Float, y: Float): Unit = {
    character.behaviorSet += "following"
    followX = x
    followY = y
    timer = 0
    deviationTimer = 0
  }


}

