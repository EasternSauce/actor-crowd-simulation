package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import org.newdawn.slick.Color

abstract class Behavior(val agent: Agent, val name: String, val color: Color) {
  def init(): Unit

  def perform(delta: Int): Unit

  def afterChangeRoom(): Unit
}
