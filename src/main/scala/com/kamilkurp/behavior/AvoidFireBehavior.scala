package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.building.{Door, Room}
import com.kamilkurp.util.ControlScheme
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class AvoidFireBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  var roomToStay: Room = null

  override def init(): Unit = {
    pickRoomToStay()
  }

  def pickRoomToStay(): Unit = {
    val knownRooms: Array[AnyRef] = agent.weightedGraph.vertexSet().toArray

    roomToStay = knownRooms(Random.nextInt(knownRooms.length)).asInstanceOf[Room]
  }

  override def perform(delta: Int): Unit = {

    if (roomToStay == agent.room) {
      agent.followModule.setFollow(agent.room.w / 2, agent.room.h/2)

      if (agent.getDistanceTo(agent.followModule.followX, agent.followModule.followY) > agent.followModule.followDistance) {
        agent.movementModule.moveTowards(agent.followModule.followX, agent.followModule.followY)
      }
      else {
        agent.movementModule.stopMoving()
      }
      return
    }

    var door: Door = null

    door = agent.doorLeadingToRoom(agent.weightedGraph, roomToStay)

    if (door != null) {
      agent.doorToEnter = door

      if (agent.controlScheme != ControlScheme.Manual) {
        agent.movementModule.moveTowards(door)
      }

    }
    else if (agent.room.meetPointList.nonEmpty) {
      agent.followModule.setFollow(agent.room.meetPointList.head.shape.getCenterX, agent.room.meetPointList.head.shape.getCenterY)

      agent.setBehavior(IdleBehavior.name)
    }
    else {
      if (agent.debug) println("encountered fire, picking another room")
      pickRoomToStay()

    }
  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {

  }

  override def afterChangeRoom(): Unit = {
    //agent.setBehavior(LeaderBehavior.name)
  }
}


object AvoidFireBehavior {
  val name: String = "avoidFire"
  val color: Color = new Color(54,68,134)
}