package com.kamilkurp.behaviors

import com.kamilkurp.agent.{Agent, AgentLeading}
import com.kamilkurp.building.{Door, Room}
import com.kamilkurp.utils.{ControlScheme, Timer}
import org.jgrapht.Graph
import org.jgrapht.graph.{DefaultEdge, SimpleGraph}
import org.newdawn.slick.geom.Vector2f

import scala.collection.mutable.ListBuffer
import scala.util.Random

class SearchExitBehavior(agent: Agent) extends Behavior(agent) {
  val deviationTimer: Timer = new Timer(500)
  val broadcastTimer: Timer = new Timer(300)


  var deviationX: Float = 0
  var deviationY: Float = 0

  var doorToEnterNext: Door = _

  override def init(): Unit = {
    decideOnDoor()
  }

  def perform(delta: Int): Unit = {
    deviationTimer.update(delta)
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
      if (deviationTimer.timedOut()) {
        deviationX = 0.3f * Random.nextFloat() - 0.15f
        deviationY = 0.3f * Random.nextFloat() - 0.15f
        deviationTimer.reset()
      }


      if (agent.controlScheme != ControlScheme.Manual) {
        val normalVector = new Vector2f(doorToEnterNext.shape.getCenterX - agent.shape.getCenterX, doorToEnterNext.shape.getCenterY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.walkAngle = normalVector.getTheta.floatValue()

        if (!agent.atDoor) {
          agent.currentVelocityX = (normalVector.x + deviationX) * agent.speed * (1f - agent.slow) * delta
          agent.currentVelocityY = (normalVector.y + deviationY) * agent.speed * (1f - agent.slow) * delta

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

      agent.setBehavior("idle")
    }
  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {

  }

  override def afterChangeRoom(): Unit = {
    decideOnDoor()
  }

  def decideOnDoor(): Unit = {
    //println("on change room")
    var door: Door = null

    door = Agent.findDoorToEnterNext(agent, agent.roomGraph)

    if (door == null) {
      // pick unknown door at random

      val doorToCorrList: ListBuffer[Door] = new ListBuffer[Door]

      for (doorInRoom <- agent.room.doorList) {
        val leadingToRoom = doorInRoom.leadingToDoor.room

        if (leadingToRoom.name.startsWith("corr")) doorToCorrList += doorInRoom

        if (leadingToRoom.meetPointList.nonEmpty || (leadingToRoom.name.startsWith("corr") && !agent.roomGraph.containsVertex(leadingToRoom))) {
          door = doorInRoom
//          println("picked unknown door")
        }
      }

      if (door == null) {
        door = doorToCorrList(Random.nextInt(doorToCorrList.length))
//        println("picked random door")
      }
    }

    doorToEnterNext = door
    //println("next door is " + door.name)
  }

}
