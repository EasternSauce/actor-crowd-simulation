package com.kamilkurp.behaviors

import com.kamilkurp.{CharacterLeadingEvacuation, ControlScheme}
import org.newdawn.slick.geom.Vector2f

import scala.util.Random
import com.kamilkurp.entities.{Character, Door}

class RunToExitBehavior extends Behavior {

  var deviationX: Float = 0
  var deviationY: Float = 0

  override var timerTimeout: Int = 3500

  var broadcastTimer: Int = 0
  var broadcastTimerTimeout: Int = 300

  var deviationTimer: Int = 0
  val deviationTimerTimeout: Int = 500

  def perform(character: Character, delta: Int): Unit = {
    timer += delta
    deviationTimer += delta
    broadcastTimer += delta

    if (broadcastTimer > broadcastTimerTimeout) {
      character.room.characterList.foreach(that => {
        if (Math.abs(that.shape.getX - character.shape.getX) <= 700
          && Math.abs(that.shape.getY - character.shape.getY) <= 700
          && that != character) {
          that.actor ! CharacterLeadingEvacuation(character, character.shape.getCenterX, character.shape.getCenterY)
        }
      })
      broadcastTimer = 0
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

      if (character.controlScheme != ControlScheme.Manual) {
        character.currentVelocityX = (normalVector.x + deviationX) * character.speed * (1f - character.slow)
        character.currentVelocityY = (normalVector.y + deviationY) * character.speed * (1f - character.slow)
      }

    }
  }
}
