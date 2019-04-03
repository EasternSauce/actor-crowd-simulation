package com.kamilkurp.behaviors

import com.kamilkurp.entities.Character
import com.kamilkurp.utils.Timer

abstract class Behavior {
  var timer: Timer

  def init(character: Character): Unit

  def perform(character: Character, delta: Int): Unit

}
