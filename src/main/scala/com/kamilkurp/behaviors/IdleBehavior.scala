package com.kamilkurp.behaviors

import com.kamilkurp.ControlScheme
import com.kamilkurp.entities.Character
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class IdleBehavior extends Behavior {
  override var timer: Int = 0
  override var timerTimeout: Int = 3000

  def perform(character: Character, delta: Int): Unit = {
    timer += delta

    if (timer > 500) {
      val inPlace = Random.nextInt(100) < 60

      timer = 0

      if (character.controlScheme != ControlScheme.Manual) {
        if (inPlace) {
          character.currentVelocityX = 0
          character.currentVelocityY = 0
        }
        else {
          character.currentVelocityX = (Random.nextInt(3) - 1) * character.speed * (1f - character.slow) * 0.8f
          character.currentVelocityY = (Random.nextInt(3) - 1) * character.speed * (1f - character.slow) * 0.8f
        }
      }


      if (character.currentVelocityX != 0 || character.currentVelocityY != 0) {
        val normalVector = new Vector2f(character.currentVelocityX, character.currentVelocityY)
        normalVector.normalise()

        character.walkAngle = normalVector.getTheta.floatValue()
      }

      if (character.room.meetPointList.nonEmpty) {
        character.followX = character.room.meetPointList.head.shape.getCenterX
        character.followY = character.room.meetPointList.head.shape.getCenterY

        character.currentBehavior = "holdMeetPoint"
      }


    }
  }

}
