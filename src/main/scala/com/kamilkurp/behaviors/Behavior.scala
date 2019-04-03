package com.kamilkurp.behaviors

import com.kamilkurp.agent.Agent
import com.kamilkurp.utils.Timer

abstract class Behavior(character: Agent) {
  var timer: Timer

  def init(): Unit

  def perform(delta: Int): Unit

  def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit
}
