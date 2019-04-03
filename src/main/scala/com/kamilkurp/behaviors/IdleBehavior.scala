package com.kamilkurp.behaviors

import com.kamilkurp.ControlScheme
import com.kamilkurp.entities.Character
import com.kamilkurp.utils.Timer
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class IdleBehavior extends Behavior {
  override var timer: Timer = new Timer(500)

  override def init(character: Character): Unit = {

  }

  def perform(character: Character, delta: Int): Unit = {
    timer.update(delta)

    if (timer.timedOut()) {
      val inPlace = Random.nextInt(100) < 60

      timer.reset()

      if (character.controlScheme != ControlScheme.Manual) {
        if (inPlace) {
          character.currentVelocityX = 0
          character.currentVelocityY = 0
        }
        else {
          character.currentVelocityX = (Random.nextInt(3) - 1) * character.speed * (1f - character.slow) * 0.8f * delta
          character.currentVelocityY = (Random.nextInt(3) - 1) * character.speed * (1f - character.slow) * 0.8f * delta
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

        character.setBehavior("holdMeetPoint")
      }


    }
  }

}
