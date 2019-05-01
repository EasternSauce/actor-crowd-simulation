package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.building.Door
import com.kamilkurp.util.ControlScheme
import org.newdawn.slick.Color

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


    if (agent.controlScheme != ControlScheme.Manual) {

      if (!agent.movementModule.beingPushed && agent.followedAgent != null) {
        if (agent.followedAgent.room == agent.room) {
          if (agent.getDistanceTo(agent.followedAgent.shape.getCenterX, agent.followedAgent.shape.getCenterY) > agent.followDistance) {
            agent.movementModule.moveTowards(agent.followedAgent.shape.getCenterX, agent.followedAgent.shape.getCenterY)
          }
          else {
            agent.movementModule.stopMoving()
          }
        }
        else {
          var door: Door = agent.doorLeadingToRoom(agent.buildingPlanGraph, agent.followedAgent.room)

          if (door != null) {
            agent.doorToEnter = door

            agent.movementModule.moveTowards(door)
          }
        }


      }
    }


    if (agent.room.meetPointList.nonEmpty) {
      agent.followX = agent.room.meetPointList.head.shape.getCenterX
      agent.followY = agent.room.meetPointList.head.shape.getCenterY

      agent.setBehavior(IdleBehavior.name)
    }


  }

  override def afterChangeRoom(): Unit = {

  }


}

object FollowBehavior {
  val name: String = "follow"
  val color: Color = Color.yellow
}

