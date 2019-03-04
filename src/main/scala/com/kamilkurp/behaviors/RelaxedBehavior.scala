package com.kamilkurp.behaviors

import com.kamilkurp.entities.Character

import scala.util.Random

class RelaxedBehavior(character: Character) extends Behavior(character) {
  override var timerTimeout: Int = 500



  def perform(delta: Int): Unit = {
    timer += delta

    if (timer > 500) {
      val inPlace = Random.nextInt(100) < 60

      timer = 0
      if (inPlace) {
        character.currentVelocityX = 0
        character.currentVelocityY = 0
      }
      else {
        character.currentVelocityX = (Random.nextInt(3) - 1) * character.speed * (1f - character.slow) * 0.8f
        character.currentVelocityY = (Random.nextInt(3) - 1) * character.speed * (1f - character.slow) * 0.8f
      }

    }
  }

}
