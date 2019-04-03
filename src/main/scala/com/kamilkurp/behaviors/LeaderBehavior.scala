package com.kamilkurp.behaviors

import com.kamilkurp.entities.{Character, Door}
import com.kamilkurp.utils.Timer
import com.kamilkurp.{CharacterLeading, ControlScheme}
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class LeaderBehavior(character: Character) extends Behavior(character) {

  override var timer: Timer = new Timer(3000)
  var deviationTimer: Timer = new Timer(500)
  var broadcastTimer: Timer = new Timer(300)

  var waitAtDoorTimer: Timer = new Timer(300)
  waitAtDoorTimer.time = waitAtDoorTimer.timeout

  var deviationX: Float = 0
  var deviationY: Float = 0

  override def init(): Unit = {

  }

  def perform(delta: Int): Unit = {
    timer.update(delta)
    deviationTimer.update(delta)
    broadcastTimer.update(delta)
    waitAtDoorTimer.update(delta)

    if (broadcastTimer.timedOut()) {
      character.room.characterList.foreach(that => {
        if (that != character) {
          that.actor ! CharacterLeading(character, character.shape.getCenterX, character.shape.getCenterY)
        }
      })
      broadcastTimer.reset()
    }


    val door: Door = character.room.evacuationDoor
    if (door != null) {
      character.doorToEnter = door
      if (deviationTimer.timedOut()) {
        deviationX = 0.3f * Random.nextFloat() - 0.15f
        deviationY = 0.3f * Random.nextFloat() - 0.15f
        deviationTimer.reset()
      }


      if (character.controlScheme != ControlScheme.Manual) {
        val normalVector = new Vector2f(door.shape.getCenterX - character.shape.getCenterX, door.shape.getCenterY - character.shape.getCenterY)
        normalVector.normalise()

        if (!character.atDoor) {
          character.currentVelocityX = (normalVector.x + deviationX) * character.speed * (1f - character.slow) * delta
          character.currentVelocityY = (normalVector.y + deviationY) * character.speed * (1f - character.slow) * delta

          if (character.getDistanceTo(character.doorToEnter.shape.getCenterX, character.doorToEnter.shape.getCenterY) < 100) {
            waitAtDoorTimer.reset()
            character.atDoor = true
          }
        }
        else {
          if (!waitAtDoorTimer.timedOut()) {
            character.currentVelocityX = 0
            character.currentVelocityY = 0
          }
          else {
            character.currentVelocityX = (normalVector.x + deviationX) * character.speed * (1f - character.slow) * delta
            character.currentVelocityY = (normalVector.y + deviationY) * character.speed * (1f - character.slow) * delta
          }
        }


      }

    }
    else if (character.room.meetPointList.nonEmpty) {
      character.followX = character.room.meetPointList.head.shape.getCenterX
      character.followY = character.room.meetPointList.head.shape.getCenterY

      character.setBehavior("holdMeetPoint")
    }
  }
}
