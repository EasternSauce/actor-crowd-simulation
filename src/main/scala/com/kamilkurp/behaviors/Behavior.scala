package com.kamilkurp.behaviors

import com.kamilkurp.entities.Character

import scala.collection.mutable

abstract class Behavior {
  var timer: Int = 0
  var timerTimeout: Int

  def perform(character: Character, delta: Int): Unit

}
