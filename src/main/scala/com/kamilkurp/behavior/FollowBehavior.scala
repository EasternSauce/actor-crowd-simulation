package com.kamilkurp.behavior

import com.kamilkurp.agent.{Agent, AgentLeading}
import com.kamilkurp.behavior_utils.Broadcasting
import com.kamilkurp.util.{Configuration, ControlScheme, Timer}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

class FollowBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) with Broadcasting {

  override def init(): Unit = {
    broadcastingInit()
  }


  def perform(delta: Int): Unit = {
    broadcastLeading()

    if (agent.followTimer.timedOut()) {
      agent.setBehavior(SearchExitBehavior.name)
      agent.followedAgent = null
      return
    }

    if (agent.lastSeenFollowedEntityTimer.timedOut()) {
      agent.lostSightOfFollowedEntity = true
      agent.lastSeenFollowedEntityTimer.stop()
    }

    val normalVector = new Vector2f(agent.followX - agent.shape.getCenterX, agent.followY - agent.shape.getCenterY)
    normalVector.normalise()

    agent.walkAngle = normalVector.getTheta.floatValue()

    if (agent.outOfWayTimer.timedOut()) {
      agent.outOfWayTimer.stop()
      agent.movingOutOfTheWay = false
      if (agent.controlScheme != ControlScheme.Manual) {

        if (agent.getDistanceTo(agent.followX, agent.followY) > agent.followDistance) {
          agent.currentVelocityX = normalVector.x * Configuration.AGENT_SPEED * (1f - agent.slow) * delta
          agent.currentVelocityY = normalVector.y * Configuration.AGENT_SPEED * (1f - agent.slow) * delta
        }
        else {
          agent.currentVelocityX = 0
          agent.currentVelocityY = 0
        }
      }
    }

    if (agent.room.meetPointList.nonEmpty) {
      agent.followX = agent.room.meetPointList.head.shape.getCenterX
      agent.followY = agent.room.meetPointList.head.shape.getCenterY

      agent.setBehavior(IdleBehavior.name)
    }


  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {
    if (that == agent.followedAgent) {
      agent.followX = posX
      agent.followY = posY
      agent.followTimer.reset()
      agent.followDistance = atDistance
    }
    else {
      agent.followX = posX
      agent.followY = posY
      agent.followDistance = atDistance
      agent.followedAgent = that
      agent.followTimer.reset()
    }
  }

  override def afterChangeRoom(): Unit = {

  }


}

object FollowBehavior {
  val name: String = "follow"
  val color: Color = Color.yellow
}

