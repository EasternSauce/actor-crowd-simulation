package com.kamilkurp.behaviors

import com.kamilkurp.ControlScheme
import com.kamilkurp.entities.Character
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class FollowingBehavior extends Behavior {



  var deviationX: Float = 0
  var deviationY: Float = 0

  override var timerTimeout: Int = 8000

  var deviationTimer: Int = 0
  val deviationTimerTimeout: Int = 500


  def perform(character: Character, delta: Int): Unit = {
    timer += delta
    deviationTimer += delta
    character.outOfWayTimer += delta

    if (timer > timerTimeout) {
      character.currentBehavior = "relaxed"
      return
    }

    if (deviationTimer > deviationTimerTimeout) {
      deviationX = 0.3f * Random.nextFloat() - 0.15f
      deviationY = 0.3f * Random.nextFloat() - 0.15f
      deviationTimer = 0
    }

    val normalVector = new Vector2f(character.followX - character.shape.getCenterX, character.followY - character.shape.getCenterY)
    normalVector.normalise()

    character.walkAngle = normalVector.getTheta.floatValue()

    if (character.outOfWayTimer > character.outOfWayTimerTimeout) {
      character.movingOutOfTheWay = false
      if (character.controlScheme != ControlScheme.Manual) {
        if (character.getDistanceTo(character.followX, character.followY) > character.followDistance) {

          character.currentVelocityX = (normalVector.x + deviationX) * character.speed * (1f - character.slow)
          character.currentVelocityY = (normalVector.y + deviationY) * character.speed * (1f - character.slow)
        }
        else {
          character.currentVelocityX = 0
          character.currentVelocityY = 0
        }

      }
    }




  }

//  def start(x: Float, y: Float): Unit = {
//    character.behaviorSet += "following"
//    followX = x
//    followY = y
//    timer = 0
//    deviationTimer = 0
//  }


}

