package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.building.{Door, Room}
import com.kamilkurp.util.ControlScheme
import org.newdawn.slick.Color
import org.newdawn.slick.geom.{Line, Shape, Transform}

import scala.util.Random

class RetrieveBelongingsBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  var belongingsRoom: Room = _

  var belongingsPosX: Float = _
  var belongingsPosY: Float = _

  var nextDoor: Door = _

  override def init(): Unit = {
    val knownRooms: Array[AnyRef] = agent.spatialModule.mentalMapGraph.vertexSet().toArray

    val randomRoom: Room = knownRooms(Random.nextInt(knownRooms.length)).asInstanceOf[Room]

    belongingsRoom = randomRoom

    belongingsPosX = Random.nextInt(randomRoom.w)
    belongingsPosY = Random.nextInt(randomRoom.h)

  }

  override def onChangeRoom(): Unit = {
    decideOnDoor()
  }

  override def perform(delta: Int): Unit = {

    if (nextDoor == null) {
      decideOnDoor()
    }


    if (agent.currentRoom == belongingsRoom) {
      if (agent.getDistanceTo(belongingsPosX, belongingsPosY) > 150) {
        agent.movementModule.moveTowards(belongingsPosX, belongingsPosY)
      }
      else {
        agent.changeBehavior(LeaderBehavior.name)
      }
    }
    else {
      if (nextDoor != null) {
        agent.intendedDoor = nextDoor

        if (agent.controlScheme != ControlScheme.Manual) {
          agent.movementModule.moveTowards(nextDoor)
        }
      }
    }

  }

  override def onSpotFire(): Unit = {

    if (agent.intendedDoor != null)

      for (door <- agent.currentRoom.doorList) {
        val leadingToRoom = door.leadingToDoor.currentRoom

        val t1 = Transform.createRotateTransform(Math.toRadians(-10).toFloat, agent.shape.getCenterX, agent.shape.getCenterY)
        val t2 = Transform.createRotateTransform(Math.toRadians(10).toFloat, agent.shape.getCenterX, agent.shape.getCenterY)
        val line = new Line(agent.shape.getCenterX, agent.shape.getCenterY, door.shape.getCenterX, door.shape.getCenterY)
        var lineLeft: Shape = new Line(agent.shape.getCenterX, agent.shape.getCenterY, door.shape.getCenterX, door.shape.getCenterY)
        lineLeft = lineLeft.transform(t1)
        var lineRight: Shape = new Line(agent.shape.getCenterX, agent.shape.getCenterY, door.shape.getCenterX, door.shape.getCenterY)
        lineRight = lineRight.transform(t2)


        var foundFire = false
        for (flames <- agent.currentRoom.flamesList) {
          if (!foundFire) {
            if (flames.shape.intersects(line) || flames.shape.intersects(lineLeft) || flames.shape.intersects(lineRight)) {
              foundFire = true
              agent.spatialModule.removeEdge(agent.currentRoom, leadingToRoom)
            }
          }

        }

      }

    decideOnDoor()
  }

  def decideOnDoor(): Unit = {
    nextDoor = agent.spatialModule.doorLeadingToRoom(agent.spatialModule.mentalMapGraph, belongingsRoom)

  }
}


object RetrieveBelongingsBehavior {
  val name: String = "retrieveBelongings"
  val color: Color = new Color(60, 124, 150)
}