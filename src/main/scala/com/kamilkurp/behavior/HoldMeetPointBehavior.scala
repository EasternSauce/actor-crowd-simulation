package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import org.newdawn.slick.Color


class HoldMeetPointBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {

  override def init(): Unit = {

  }

  override def perform(delta: Int): Unit = {
    agent.movementModule.moveTowards(agent.followX, agent.followY)
  }

  override def onChangeRoom(): Unit = {

  }

  override def onSpotFire(): Unit = {

  }
}


object HoldMeetPointBehavior {
  val name: String = "holdMeetPoint"
  val color: Color = Color.cyan
}