package com.kamilkurp.behaviors

import com.kamilkurp.Timer
import com.kamilkurp.entities.Character

abstract class Behavior {
  var timer: Timer

  def perform(character: Character, delta: Int): Unit

}
