package com.kamilkurp.behaviors
import com.kamilkurp.entities
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class HoldMeetPointBehavior extends Behavior {
  override var timerTimeout: Int = 0

  var deviationX: Float = 0
  var deviationY: Float = 0

  var deviationTimer: Int = 0
  val deviationTimerTimeout: Int = 500


  override def perform(character: entities.Character, delta: Int): Unit = {

    println(character.name + " holding meet point at " + character.followX + " " + character.followY)

    val normalVector = new Vector2f(character.followX - character.shape.getCenterX, character.followY - character.shape.getCenterY)
    normalVector.normalise()

    if (deviationTimer > deviationTimerTimeout) {
      deviationX = 0.3f * Random.nextFloat() - 0.15f
      deviationY = 0.3f * Random.nextFloat() - 0.15f
      deviationTimer = 0
    }
    character.currentVelocityX = (normalVector.x + deviationX) * character.speed * (1f - character.slow)
    character.currentVelocityY = (normalVector.y + deviationY) * character.speed * (1f - character.slow)
  }
}
