package com.kamilkurp.behaviors

import org.newdawn.slick.geom.Vector2f

import scala.util.Random
import com.kamilkurp.entities.{Character, Door}

class RunToExitBehavior(character: Character) extends Behavior(character) {

  var deviationX: Float = 0
  var deviationY: Float = 0

  override var timerTimeout: Int = 3500

  var deviationTimer: Int = 0
  val deviationTimerTimeout: Int = 500

  def perform(delta: Int): Unit = {
    timer += delta
    deviationTimer += delta

    if (character.room.evacuationDoor == null) {
      character.behaviorSet -= "runToExit"
      return
    }

    val door: Door = character.room.evacuationDoor
    if (door != null) {
      if (deviationTimer > deviationTimerTimeout) {
        deviationX = 0.3f * Random.nextFloat() - 0.15f
        deviationY = 0.3f * Random.nextFloat() - 0.15f
        deviationTimer = 0
      }

      val normalVector = new Vector2f(door.posX - character.shape.getX, door.posY - character.shape.getY)
      normalVector.normalise()

      character.walkAngle = normalVector.getTheta.floatValue()

      character.currentVelocityX = (normalVector.x + deviationX) * character.speed * (1f - character.slow)
      character.currentVelocityY = (normalVector.y + deviationY) * character.speed * (1f - character.slow)
    }
  }
}
