package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.building.Door
import com.kamilkurp.util.ControlScheme
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

import scala.collection.mutable.ListBuffer
import scala.util.Random

class SearchExitBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  var doorToEnterNext: Door = _

  override def init(): Unit = {
    decideOnDoor()
  }

  def decideOnDoor(): Unit = {
    var door: Door = null

    door = agent.spatialModule.findDoorToEnterNext()

    if (door == null) {

      val doorToCorrList: ListBuffer[Door] = new ListBuffer[Door]

      for (doorInRoom <- agent.currentRoom.doorList) {
        val leadingToRoom = doorInRoom.leadingToDoor.currentRoom

        if (leadingToRoom.name.startsWith("corr")) doorToCorrList += doorInRoom

        if (leadingToRoom.meetPointList.nonEmpty || (leadingToRoom.name.startsWith("corr") && !agent.spatialModule.mentalMapGraph.containsVertex(leadingToRoom))) {
          door = doorInRoom
        }
      }

      if (door == null) {
        door = doorToCorrList(Random.nextInt(doorToCorrList.length))
      }
    }

    doorToEnterNext = door
  }

  def perform(delta: Int): Unit = {

    if (doorToEnterNext != null && agent.currentRoom.meetPointList.isEmpty) {
      agent.doorToEnter = doorToEnterNext

      if (agent.controlScheme != ControlScheme.Manual) {
        val normalVector = new Vector2f(doorToEnterNext.shape.getCenterX - agent.shape.getCenterX, doorToEnterNext.shape.getCenterY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.movementModule.walkAngle = normalVector.getTheta.floatValue()

        if (!agent.movementModule.beingPushed) {
          agent.movementModule.moveTowards(doorToEnterNext)
        }

      }

    }
    else if (agent.currentRoom.meetPointList.nonEmpty) {
      agent.followX = agent.currentRoom.meetPointList.head.shape.getCenterX
      agent.followY = agent.currentRoom.meetPointList.head.shape.getCenterY


      agent.setBehavior(IdleBehavior.name)
    }
  }

  override def afterChangeRoom(): Unit = {
    decideOnDoor()
  }

}


object SearchExitBehavior {
  val name: String = "searchExit"
  val color: Color = Color.orange
}