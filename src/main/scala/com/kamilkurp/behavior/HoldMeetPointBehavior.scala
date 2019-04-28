package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.util.Configuration
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f


class HoldMeetPointBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {

  override def init(): Unit = {

  }

  override def perform(delta: Int): Unit = {
    agent.movementModule.moveTowards(agent.followModule.followX, agent.followModule.followY)
  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {

  }

  override def afterChangeRoom(): Unit = {

  }
}


object HoldMeetPointBehavior {
  val name: String = "holdMeetPoint"
  val color: Color = Color.cyan
}