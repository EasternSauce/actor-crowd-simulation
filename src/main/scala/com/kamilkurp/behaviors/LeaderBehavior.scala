package com.kamilkurp.behaviors

import com.kamilkurp.entities.{Character, Door}
import com.kamilkurp.{CharacterLeading, ControlScheme, Timer}
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class LeaderBehavior extends Behavior {

  override var timer: Timer = new Timer(3000)
  var deviationTimer: Timer = new Timer(500)
  var broadcastTimer: Timer = new Timer(300)

  var deviationX: Float = 0
  var deviationY: Float = 0


  def perform(character: Character, delta: Int): Unit = {
    timer.update(delta)
    deviationTimer.update(delta)
    broadcastTimer.update(delta)

    if (broadcastTimer.timedOut()) {
      character.room.characterList.foreach(that => {
        if ( /*Math.abs(that.shape.getX - character.shape.getX) <= 700
          && Math.abs(that.shape.getY - character.shape.getY) <= 700
          && */ that != character) {
          that.actor ! CharacterLeading(character, character.shape.getCenterX, character.shape.getCenterY)
        }
      })
      broadcastTimer.reset()
    }


    val door: Door = character.room.evacuationDoor
    if (door != null) {
      if (deviationTimer.timedOut()) {
        deviationX = 0.3f * Random.nextFloat() - 0.15f
        deviationY = 0.3f * Random.nextFloat() - 0.15f
        deviationTimer.reset()
      }


      if (character.controlScheme != ControlScheme.Manual) {
        val normalVector = new Vector2f(door.posX - character.shape.getX, door.posY - character.shape.getY)
        normalVector.normalise()

        character.walkAngle = normalVector.getTheta.floatValue()

        character.currentVelocityX = (normalVector.x + deviationX) * character.speed * (1f - character.slow)
        character.currentVelocityY = (normalVector.y + deviationY) * character.speed * (1f - character.slow)
      }

    }
    else if (character.room.meetPointList.nonEmpty) {
      character.followX = character.room.meetPointList.head.shape.getCenterX
      character.followY = character.room.meetPointList.head.shape.getCenterY

      character.currentBehavior = "holdMeetPoint"
    }
  }
}
