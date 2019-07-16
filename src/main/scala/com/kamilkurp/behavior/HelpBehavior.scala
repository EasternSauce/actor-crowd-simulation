package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import org.newdawn.slick.Color

class HelpBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  override def init(): Unit = {

  }

  override def perform(delta: Int): Unit = {
    if (!agent.movementModule.beingPushed && agent.helpedAgent != null) {
      if (agent.helpedAgent.currentRoom == agent.currentRoom) {
        if (agent.getDistanceTo(agent.helpedAgent.shape.getCenterX, agent.helpedAgent.shape.getCenterY) > 30) {
          agent.movementModule.moveTowards(agent.helpedAgent.shape.getCenterX, agent.helpedAgent.shape.getCenterY)

        }
      }
      else {
        agent.movementModule.stopMoving()
      }
    }

    if (!agent.helpedAgent.movementModule.isTripped) {
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