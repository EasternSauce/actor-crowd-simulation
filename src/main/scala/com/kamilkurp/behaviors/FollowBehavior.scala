package com.kamilkurp.behaviors

import com.kamilkurp.agent.{Agent, AgentLeading}
import com.kamilkurp.utils.{ControlScheme, Timer}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class FollowBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  val deviationTimer: Timer = new Timer(500)
  val broadcastTimer: Timer = new Timer(300)
  var deviationX: Float = 0
  var deviationY: Float = 0

  override def init(): Unit = {

  }


  def perform(delta: Int): Unit = {
    agent.followTimer.update(delta)
    deviationTimer.update(delta)
    broadcastTimer.update(delta)
    agent.outOfWayTimer.update(delta)

    if (!agent.lostSightOfFollowedEntity) agent.lastSeenFollowedEntityTimer.update(delta)

    if (broadcastTimer.timedOut()) {
      agent.room.agentList.foreach(that => {
        if (that != agent) {
          that.actor ! AgentLeading(agent, agent.shape.getCenterX, agent.shape.getCenterY)
        }
      })
      broadcastTimer.reset()
    }

    if (agent.followTimer.timedOut()) {
      agent.setBehavior(SearchExitBehavior.name)
      agent.followedAgent = null
      return
    }

    if (agent.lastSeenFollowedEntityTimer.timedOut()) {
      agent.lostSightOfFollowedEntity = true
      agent.lastSeenFollowedEntityTimer.reset()
    }

    if (deviationTimer.timedOut()) {
      deviationX = 0.3f * Random.nextFloat() - 0.15f
      deviationY = 0.3f * Random.nextFloat() - 0.15f
      deviationTimer.reset()
    }

    val normalVector = new Vector2f(agent.followX - agent.shape.getCenterX, agent.followY - agent.shape.getCenterY)
    normalVector.normalise()

    agent.walkAngle = normalVector.getTheta.floatValue()

    if (agent.outOfWayTimer.timedOut()) {
      agent.movingOutOfTheWay = false
      if (agent.controlScheme != ControlScheme.Manual) {

        if (agent.getDistanceTo(agent.followX, agent.followY) > agent.followDistance) {
          agent.currentVelocityX = (normalVector.x + deviationX) * agent.speed * (1f - agent.slow) * delta
          agent.currentVelocityY = (normalVector.y + deviationY) * agent.speed * (1f - agent.slow) * delta
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
  val name: String = FollowBehavior.name
  val color: Color = Color.yellow
}

