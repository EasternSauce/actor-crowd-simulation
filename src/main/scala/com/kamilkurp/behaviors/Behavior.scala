package com.kamilkurp.behaviors

import com.kamilkurp.entities.Character

abstract class Behavior {
  var timer: Int
  var timerTimeout: Int

  def perform(character: Character, delta: Int): Unit

}
