package com.kamilkurp.behaviors

import com.kamilkurp.agent.Agent

abstract class Behavior(agent: Agent) {


  def init(): Unit

  def perform(delta: Int): Unit

  def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit

  def afterChangeRoom(): Unit
}
