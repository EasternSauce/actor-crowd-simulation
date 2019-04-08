package com.kamilkurp.behaviors

import com.kamilkurp.agent.Agent
import org.newdawn.slick.Color

abstract class Behavior(agent: Agent, name: String, color: Color) {
  def init(): Unit

  def perform(delta: Int): Unit

  def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit

  def afterChangeRoom(): Unit
}
