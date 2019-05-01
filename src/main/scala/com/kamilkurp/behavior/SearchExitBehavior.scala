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

  def perform(delta: Int): Unit = {

    if (doorToEnterNext != null && agent.room.meetPointList.isEmpty) {
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
    else if (agent.room.meetPointList.nonEmpty) {
      agent.followX = agent.room.meetPointList.head.shape.getCenterX
      agent.followY = agent.room.meetPointList.head.shape.getCenterY


      agent.setBehavior(IdleBehavior.name)
    }
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

        if (leadingToRoom.meetPointList.nonEmpty || (leadingToRoom.name.startsWith("corr") && !agent.mentalMapGraph.containsVertex(leadingToRoom))) {
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