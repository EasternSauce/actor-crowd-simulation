package com.kamilkurp.behaviors

import com.kamilkurp.entities.Character
import com.kamilkurp.{Timer}
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class HoldMeetPointBehavior extends Behavior {

  override def init(character: Character): Unit = {

  }

  override var timer: Timer = new Timer(3000)
  var deviationTimer: Timer = new Timer(500)

  var deviationX: Float = 0
  var deviationY: Float = 0

  override def perform(character: Character, delta: Int): Unit = {
    val normalVector = new Vector2f(character.followX - character.shape.getCenterX, character.followY - character.shape.getCenterY)
    normalVector.normalise()

    if (deviationTimer.timedOut()) {
      deviationX = 0.3f * Random.nextFloat() - 0.15f
      deviationY = 0.3f * Random.nextFloat() - 0.15f
      deviationTimer.reset()
    }
    character.currentVelocityX = (normalVector.x + deviationX) * character.speed * (1f - character.slow)
    character.currentVelocityY = (normalVector.y + deviationY) * character.speed * (1f - character.slow)
  }
}
