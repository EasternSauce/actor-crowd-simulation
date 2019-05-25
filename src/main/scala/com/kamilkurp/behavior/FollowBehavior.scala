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
      agent.changeBehavior(SearchExitBehavior.name)
      agent.followedAgent = null
      return
    }


    if (agent.controlScheme != ControlScheme.Manual) {

      if (!agent.movementModule.beingPushed && agent.followedAgent != null) {
        if (agent.followedAgent.currentRoom == agent.currentRoom) {
          if (agent.getDistanceTo(agent.followedAgent.shape.getCenterX, agent.followedAgent.shape.getCenterY) > agent.followDistance) {
            agent.movementModule.moveTowards(agent.followedAgent.shape.getCenterX, agent.followedAgent.shape.getCenterY)
          }
          else {
            agent.movementModule.stopMoving()
          }
        }
        else {
          var door: Door = agent.spatialModule.doorLeadingToRoom(agent.buildingPlanGraph, agent.followedAgent.currentRoom)

          if (door != null) {
            agent.intendedDoor = door

            agent.movementModule.moveTowards(door)
          }
        }


      }
    }


    if (agent.currentRoom.meetPointList.nonEmpty) {
      agent.followX = agent.currentRoom.meetPointList.head.shape.getCenterX
      agent.followY = agent.currentRoom.meetPointList.head.shape.getCenterY

      agent.changeBehavior(IdleBehavior.name)
    }


  }

  override def onChangeRoom(): Unit = {

  }

  override def onSpotFire(): Unit = {

  }

}

object FollowBehavior {
  val name: String = "follow"
  val color: Color = Color.yellow
}

