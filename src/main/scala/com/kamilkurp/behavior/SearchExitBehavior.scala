package com.kamilkurp.behavior

import com.kamilkurp.agent.{Agent, AgentLeading}
import com.kamilkurp.building.Door
import com.kamilkurp.util.{Configuration, ControlScheme, Timer}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

import scala.collection.mutable.ListBuffer
import scala.util.Random

class SearchExitBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  val broadcastTimer: Timer = new Timer(Configuration.AGENT_BROADCAST_TIMER)

  var doorToEnterNext: Door = _

  override def init(): Unit = {
    decideOnDoor()
  }

  def perform(delta: Int): Unit = {

    if (doorToEnterNext != null && agent.room.meetPointList.isEmpty) {
      agent.doorToEnter = doorToEnterNext

      if (agent.controlScheme != ControlScheme.Manual) {
        val normalVector = new Vector2f(doorToEnterNext.shape.getCenterX - agent.shape.getCenterX, doorToEnterNext.shape.getCenterY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.movementModule.walkAngle = normalVector.getTheta.floatValue()

        if (!agent.movementModule.beingPushed) {
          if (!agent.atDoor) agent.movementModule.moveTowards(doorToEnterNext)
          else agent.movementModule.stopMoving()
        }

      }

    }
    else if (agent.room.meetPointList.nonEmpty) {
      agent.followModule.setFollow(agent.room.meetPointList.head.shape.getCenterX, agent.room.meetPointList.head.shape.getCenterY)

      agent.behaviorModule.setBehavior(IdleBehavior.name)
    }
  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {
    agent.behaviorModule.setBehavior(FollowBehavior.name)

    agent.followModule.setFollow(posX, posY)
    agent.followModule.followDistance = atDistance
    agent.followModule.followedAgent = that
    agent.followModule.followTimer.reset()
  }

  override def afterChangeRoom(): Unit = {
    decideOnDoor()
  }

  def decideOnDoor(): Unit = {
    var door: Door = null

    door = agent.findDoorToEnterNext()

    if (door == null) {

      val doorToCorrList: ListBuffer[Door] = new ListBuffer[Door]

      for (doorInRoom <- agent.room.doorList) {
        val leadingToRoom = doorInRoom.leadingToDoor.room

        if (leadingToRoom.name.startsWith("corr")) doorToCorrList += doorInRoom

        if (leadingToRoom.meetPointList.nonEmpty || (leadingToRoom.name.startsWith("corr") && !agent.roomGraph.containsVertex(leadingToRoom))) {
          door = doorInRoom
        }
      }

      if (door == null) {
        door = doorToCorrList(Random.nextInt(doorToCorrList.length))
      }
    }

    doorToEnterNext = door
  }

}


object SearchExitBehavior {
  val name: String = "searchExit"
  val color: Color = Color.orange
}