package com.kamilkurp.behaviors

import com.kamilkurp.agent.Agent
import com.kamilkurp.utils.Configuration
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f


class HoldMeetPointBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {

  override def init(): Unit = {

  }

  override def perform(delta: Int): Unit = {
    val normalVector = new Vector2f(agent.followX - agent.shape.getCenterX, agent.followY - agent.shape.getCenterY)
    normalVector.normalise()

    agent.currentVelocityX = normalVector.x * Configuration.AGENT_SPEED * (1f - agent.slow) * delta
    agent.currentVelocityY = normalVector.y * Configuration.AGENT_SPEED * (1f - agent.slow) * delta
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