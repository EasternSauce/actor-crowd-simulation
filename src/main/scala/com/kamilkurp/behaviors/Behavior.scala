package com.kamilkurp.behaviors

import com.kamilkurp.entities.Character

abstract class Behavior(character: Character) {
  var timer: Int = 0
  var timerTimeout: Int

  def increaseTimer(delta: Int) {
    timer += delta
  }

  def perform(): Unit
}
