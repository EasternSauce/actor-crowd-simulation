package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.building.{Door, Room}
import com.kamilkurp.util.ControlScheme
import org.newdawn.slick.Color
import org.newdawn.slick.geom.{Line, Shape, Transform}

import scala.util.Random

class AvoidFireBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  var roomToStay: Room = null

  override def init(): Unit = {
    pickRoomToStay()
  }

  def pickRoomToStay(): Unit = {
    val knownRooms: Array[AnyRef] = agent.spatialModule.mentalMapGraph.vertexSet().toArray

    roomToStay = null
    var i = 0
    while (i < 3) {

      val roomCandidate = knownRooms(Random.nextInt(knownRooms.length)).asInstanceOf[Room]

      if (agent.spatialModule.shortestPath(agent.spatialModule.mentalMapGraph, agent.currentRoom, roomCandidate) != null) {
        roomToStay = roomCandidate
        i = 3
      }

      i = i + 1
    }

    if (roomToStay == null) {
      roomToStay = agent.currentRoom
      agent.followX = agent.currentRoom.w / 2
      agent.followY = agent.currentRoom.h / 2
      agent.followDistance = 40
    }
  }

  override def perform(delta: Int): Unit = {

    if (roomToStay == agent.currentRoom) {
      if (agent.currentRoom.flamesList.nonEmpty) {
        var pickedDoor = agent.spatialModule.findDoorToEnterNext()

        for (door <- agent.currentRoom.doorList) {
          val t1 = Transform.createRotateTransform(Math.toRadians(-10).toFloat, agent.shape.getCenterX, agent.shape.getCenterY)
          val t2 = Transform.createRotateTransform(Math.toRadians(10).toFloat, agent.shape.getCenterX, agent.shape.getCenterY)
          var line = new Line(agent.shape.getCenterX, agent.shape.getCenterY, door.shape.getCenterX, door.shape.getCenterY)
          var lineLeft: Shape = new Line(agent.shape.getCenterX, agent.shape.getCenterY, door.shape.getCenterX, door.shape.getCenterY)
          lineLeft = lineLeft.transform(t1)
          var lineRight: Shape = new Line(agent.shape.getCenterX, agent.shape.getCenterY, door.shape.getCenterX, door.shape.getCenterY)
          lineRight = lineRight.transform(t2)

          for (flames <- agent.currentRoom.flamesList) {
            if (roomToStay == agent.currentRoom) {
              if (!(flames.shape.intersects(line) || flames.shape.intersects(lineLeft) || flames.shape.intersects(lineRight))) {
                roomToStay = door.leadingToDoor.currentRoom
                agent.intendedDoor = door
                agent.followX = door.shape.getCenterX
                agent.followY = door.shape.getCenterY
                agent.followDistance = 0
              }
            }


          }

        }

        if (pickedDoor != null) roomToStay = pickedDoor.leadingToDoor.currentRoom
      }

      if (agent.getDistanceTo(agent.followX, agent.followY) > agent.followDistance) {
        agent.movementModule.moveTowards(agent.followX, agent.followY)
      }
      else {
        agent.movementModule.stopMoving()
      }
      return
    }

    var door: Door = null

    door = agent.spatialModule.doorLeadingToRoom(agent.spatialModule.mentalMapGraph, roomToStay)

    if (door != null) {
      agent.intendedDoor = door

      if (agent.controlScheme != ControlScheme.Manual) {
        agent.movementModule.moveTowards(door)

      }

    }
    else if (agent.currentRoom.meetPointList.nonEmpty) {
      agent.followX = agent.currentRoom.meetPointList.head.shape.getCenterX
      agent.followY = agent.currentRoom.meetPointList.head.shape.getCenterY

      agent.setBehavior(IdleBehavior.name)
    }
    else {
      pickRoomToStay()

    }
  }

  override def onChangeRoom(): Unit = {
    if (roomToStay == agent.currentRoom) {
      agent.followX = agent.currentRoom.w / 2
      agent.followY = agent.currentRoom.h / 2
      agent.followDistance = 40
    }
  }

  override def onSpotFire(): Unit = {

  }
}


object AvoidFireBehavior {
  val name: String = "avoidFire"
  val color: Color = new Color(54, 68, 134)
}