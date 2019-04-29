package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.behavior_utils.Broadcasting
import com.kamilkurp.building.Door
import com.kamilkurp.util.{Configuration, ControlScheme, Timer}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

class LeaderBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) with Broadcasting {

  val waitAtDoorTimer: Timer = new Timer(Configuration.WAIT_AT_DOOR_TIMER)

  override def init(): Unit = {
    broadcastingInit()
  }

  def perform(delta: Int): Unit = {
    broadcastLeading()

    var door: Door = agent.findDoorToEnterNext()

    if (door != null) {
      agent.doorToEnter = door

      if (agent.controlScheme != ControlScheme.Manual) {
        if (!agent.atDoor) {
          agent.movementModule.moveTowards(door)

          if (agent.getDistanceTo(agent.doorToEnter.shape.getCenterX, agent.doorToEnter.shape.getCenterY) < 100) {
            waitAtDoorTimer.reset()
            waitAtDoorTimer.start()
            agent.atDoor = true
          }
        }
        else {
          agent.movementModule.moveTowards(door)
        }
      }

    }
    else if (agent.room.meetPointList.nonEmpty) {
      agent.followX = agent.room.meetPointList.head.shape.getCenterX
      agent.followY = agent.room.meetPointList.head.shape.getCenterY

      agent.setBehavior(IdleBehavior.name)
    }
    else {
      agent.setBehavior(AvoidFireBehavior.name)
    }
  }


  override def afterChangeRoom(): Unit = {

  }
}


object LeaderBehavior {
  val name: String = "leader"
  val color: Color = Color.red
}