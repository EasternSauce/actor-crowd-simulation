package com.kamilkurp.behavior

import com.kamilkurp.agent.{Agent, AgentLeading}
import com.kamilkurp.behavior_utils.Broadcasting
import com.kamilkurp.util.{Configuration, ControlScheme, Timer}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

class FollowBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {

  override def init(): Unit = {
  }


  def perform(delta: Int): Unit = {
    if (agent.followTimer.timedOut()) {
      agent.followTimer.stop()
      agent.setBehavior(SearchExitBehavior.name)
      agent.followedAgent = null
      return
    }

    if (agent.lastSeenFollowedEntityTimer.timedOut()) {
      agent.lostSightOfFollowedEntity = true
      agent.lastSeenFollowedEntityTimer.stop()
    }

    if (agent.outOfWayTimer.timedOut()) {
      agent.outOfWayTimer.stop()
      agent.movingOutOfTheWay = false
      if (agent.controlScheme != ControlScheme.Manual) {

        if (!agent.beingPushed) {
          if (agent.getDistanceTo(agent.followX, agent.followY) > agent.followDistance) {
            agent.moveTowards(agent.followX, agent.followY, delta)
          }
          else {
            agent.stopMoving()
          }
        }
      }
    }

    if (agent.room.meetPointList.nonEmpty) {
      agent.setFollow(agent.room.meetPointList.head.shape.getCenterX, agent.room.meetPointList.head.shape.getCenterY)

      agent.setBehavior(IdleBehavior.name)
    }


  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {
    agent.setFollow(posX, posY)
    agent.followTimer.reset()
    agent.followDistance = atDistance
    if (that != agent.followedAgent) agent.followedAgent = that
  }

  override def afterChangeRoom(): Unit = {

  }


}

object FollowBehavior {
  val name: String = "follow"
  val color: Color = Color.yellow
}

