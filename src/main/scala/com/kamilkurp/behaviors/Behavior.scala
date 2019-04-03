package com.kamilkurp.behaviors

import com.kamilkurp.entities.Character
import com.kamilkurp.utils.Timer

abstract class Behavior(character: Character) {
  var timer: Timer

  def init(): Unit

  def perform(delta: Int): Unit

}
