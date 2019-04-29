package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import org.newdawn.slick.Color

class StationaryBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  override def init(): Unit = {

  }

  override def perform(delta: Int): Unit = {

  }

  override def afterChangeRoom(): Unit = {

  }
}


object StationaryBehavior {
  val name: String = "stationary"
  val color: Color = Color.blue
}