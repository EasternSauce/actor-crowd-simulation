package com.kamilkurp.behaviors

import com.kamilkurp.agent.{Agent, AgentLeading}
import com.kamilkurp.building.Door
import com.kamilkurp.utils.{Configuration, ControlScheme, Timer}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

import scala.collection.mutable.ListBuffer
import scala.util.Random

class SearchExitBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  val broadcastTimer: Timer = new Timer(300)

  var doorToEnterNext: Door = _

  override def init(): Unit = {
    decideOnDoor()
  }

  def perform(delta: Int): Unit = {
    broadcastTimer.update(delta)

    if (broadcastTimer.timedOut()) {
      agent.room.agentList.foreach(that => {
        if (that != agent) {
          that.actor ! AgentLeading(agent, agent.shape.getCenterX, agent.shape.getCenterY)
        }
      })
      broadcastTimer.reset()
    }

    if (doorToEnterNext != null && agent.room.meetPointList.isEmpty) {
      agent.doorToEnter = doorToEnterNext

      if (agent.controlScheme != ControlScheme.Manual) {
        val normalVector = new Vector2f(doorToEnterNext.shape.getCenterX - agent.shape.getCenterX, doorToEnterNext.shape.getCenterY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.walkAngle = normalVector.getTheta.floatValue()

        if (!agent.atDoor) {
          agent.currentVelocityX = normalVector.x * Configuration.AGENT_SPEED * (1f - agent.slow) * delta
          agent.currentVelocityY = normalVector.y * Configuration.AGENT_SPEED * (1f - agent.slow) * delta

        }
        else {
          agent.currentVelocityX = 0
          agent.currentVelocityY = 0
        }
      }

    }
    else if (agent.room.meetPointList.nonEmpty) {
      agent.followX = agent.room.meetPointList.head.shape.getCenterX
      agent.followY = agent.room.meetPointList.head.shape.getCenterY

      agent.setBehavior(IdleBehavior.name)
    }
  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {

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