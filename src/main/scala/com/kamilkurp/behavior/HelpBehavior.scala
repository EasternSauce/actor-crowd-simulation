package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import org.newdawn.slick.Color

class HelpBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  override def init(): Unit = {

  }

  override def perform(delta: Int): Unit = {
    if (!agent.movementModule.beingPushed && agent.helpingAgent != null) {
      if (agent.followedAgent.currentRoom == agent.currentRoom) {
        agent.movementModule.moveTowards(agent.followedAgent.shape.getCenterX, agent.followedAgent.shape.getCenterY)
      }
      else {
        agent.movementModule.stopMoving()
      }
    }

    if (!agent.helpingAgent.movementModule.isTripped) {
      agent.changeBehavior(agent.behaviorModule.previousBehavior)
    }
  }

  override def onChangeRoom(): Unit = {

  }

  override def onSpotFire(): Unit = {

  }
}


object HelpBehavior {
  val name: String = "help"
  val color: Color = new Color(242,195,50)
}