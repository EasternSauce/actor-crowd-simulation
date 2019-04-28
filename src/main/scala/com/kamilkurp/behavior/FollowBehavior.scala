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
    if (agent.followModule.followTimer.timedOut()) {
      agent.followModule.followTimer.stop()
      agent.behaviorModule.setBehavior(SearchExitBehavior.name)
      agent.followModule.followedAgent = null
      return
    }

    if (agent.followModule.lastSeenFollowedEntityTimer.timedOut()) {
      agent.followModule.lostSightOfFollowedEntity = true
      agent.followModule.lastSeenFollowedEntityTimer.stop()
    }

    if (agent.outOfWayTimer.timedOut()) {
      agent.outOfWayTimer.stop()
      agent.movingOutOfTheWay = false
      if (agent.controlScheme != ControlScheme.Manual) {

        if (!agent.movementModule.beingPushed) {
          if (agent.getDistanceTo(agent.followModule.followX, agent.followModule.followY) > agent.followModule.followDistance) {
            agent.movementModule.moveTowards(agent.followModule.followX, agent.followModule.followY)
          }
          else {
            agent.movementModule.stopMoving()
          }
        }
      }
    }

    if (agent.room.meetPointList.nonEmpty) {
      agent.followModule.setFollow(agent.room.meetPointList.head.shape.getCenterX, agent.room.meetPointList.head.shape.getCenterY)

      agent.behaviorModule.setBehavior(IdleBehavior.name)
    }


  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {
    agent.followModule.setFollow(posX, posY)
    agent.followModule.followTimer.reset()
    agent.followModule.followDistance = atDistance
    if (that != agent.followModule.followedAgent) agent.followModule.followedAgent = that
  }

  override def afterChangeRoom(): Unit = {

  }


}

object FollowBehavior {
  val name: String = "follow"
  val color: Color = Color.yellow
}

