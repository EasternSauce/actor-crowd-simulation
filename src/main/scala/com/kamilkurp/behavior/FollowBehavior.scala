package com.kamilkurp.behavior

import com.kamilkurp.agent.{Agent, AgentLeading}
import com.kamilkurp.behavior_utils.Broadcasting
import com.kamilkurp.building.Door
import com.kamilkurp.util.{Configuration, ControlScheme, Timer}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

class FollowBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {

  override def init(): Unit = {
  }


  def perform(delta: Int): Unit = {
    if (agent.followModule.followTimer.timedOut()) {
      agent.followModule.followTimer.stop()
      agent.setBehavior(SearchExitBehavior.name)
      agent.followModule.followedAgent = null
      return
    }


    if (agent.controlScheme != ControlScheme.Manual) {

      if (!agent.movementModule.beingPushed && agent.followModule.followedAgent != null) {
        if (agent.followModule.followedAgent.room == agent.room) {
          if (agent.getDistanceTo(agent.followModule.followedAgent.shape.getCenterX, agent.followModule.followedAgent.shape.getCenterY) > agent.followModule.followDistance) {
            agent.movementModule.moveTowards(agent.followModule.followedAgent.shape.getCenterX, agent.followModule.followedAgent.shape.getCenterY)
          }
          else {
            agent.movementModule.stopMoving()
          }
        }
        else {
          var door: Door = agent.doorLeadingToRoom(agent.weightedGraph, agent.followModule.followedAgent.room)

          if (door != null) {
            agent.doorToEnter = door

            agent.movementModule.moveTowards(door)
          }
        }


      }
    }


    if (agent.room.meetPointList.nonEmpty) {
      agent.followModule.setFollow(agent.room.meetPointList.head.shape.getCenterX, agent.room.meetPointList.head.shape.getCenterY)

      agent.setBehavior(IdleBehavior.name)
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

