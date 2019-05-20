package com.kamilkurp.behavior

import com.kamilkurp.agent.{Agent, AgentLeading, FireLocationInfo}
import com.kamilkurp.building.Door
import com.kamilkurp.util.{Configuration, ControlScheme, Timer}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.{Line, Shape, Transform}

class LeaderBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {

  var broadcastTimer: Timer = _
  var fireLocationInfoTimer: Timer = _

  var door: Door = _
  override def init(): Unit = {
    broadcastTimer = new Timer(Configuration.agentBroadcastTimer)
    broadcastTimer.start()
    fireLocationInfoTimer = new Timer(500)
    fireLocationInfoTimer.start()

  }

  override def onChangeRoom(): Unit = {
    door = agent.spatialModule.findDoorToEnterNext()
  }

  override def perform(delta: Int): Unit = {
    agent.broadcast(AgentLeading(agent), broadcastTimer)
    agent.broadcast(FireLocationInfo(agent.spatialModule.knownFireLocations), fireLocationInfoTimer)

    if (door == null) {
      door = agent.spatialModule.findDoorToEnterNext()
    }

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
      agent.setBehavior(AvoidFireBehavior.name)
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

    door = agent.spatialModule.findDoorToEnterNext()
  }
}


object LeaderBehavior {
  val name: String = "leader"
  val color: Color = Color.red
}