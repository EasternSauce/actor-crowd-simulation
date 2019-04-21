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
    if (agent.followManager.followTimer.timedOut()) {
      agent.followManager.followTimer.stop()
      agent.behaviorManager.setBehavior(SearchExitBehavior.name)
      agent.followManager.followedAgent = null
      return
    }

    if (agent.followManager.lastSeenFollowedEntityTimer.timedOut()) {
      agent.followManager.lostSightOfFollowedEntity = true
      agent.followManager.lastSeenFollowedEntityTimer.stop()
    }

    if (agent.outOfWayTimer.timedOut()) {
      agent.outOfWayTimer.stop()
      agent.movingOutOfTheWay = false
      if (agent.controlScheme != ControlScheme.Manual) {

        if (!agent.beingPushed) {
          if (agent.getDistanceTo(agent.followManager.followX, agent.followManager.followY) > agent.followManager.followDistance) {
            agent.moveTowards(agent.followManager.followX, agent.followManager.followY)
          }
          else {
            agent.stopMoving()
          }
        }
      }
    }

    if (agent.room.meetPointList.nonEmpty) {
      agent.followManager.setFollow(agent.room.meetPointList.head.shape.getCenterX, agent.room.meetPointList.head.shape.getCenterY)

      agent.behaviorManager.setBehavior(IdleBehavior.name)
    }


  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {
    agent.followManager.setFollow(posX, posY)
    agent.followManager.followTimer.reset()
    agent.followManager.followDistance = atDistance
    if (that != agent.followManager.followedAgent) agent.followManager.followedAgent = that
  }

  override def afterChangeRoom(): Unit = {

  }


}

object FollowBehavior {
  val name: String = "follow"
  val color: Color = Color.yellow
}

