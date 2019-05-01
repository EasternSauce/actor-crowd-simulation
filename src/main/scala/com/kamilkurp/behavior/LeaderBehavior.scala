package com.kamilkurp.behavior

import com.kamilkurp.agent.{Agent, AgentLeading, FireLocationInfo}
import com.kamilkurp.building.Door
import com.kamilkurp.util.{Configuration, ControlScheme, Timer}
import org.newdawn.slick.Color

class LeaderBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {

  var broadcastTimer: Timer = _
  var fireLocationInfoTimer: Timer = _

  override def init(): Unit = {
    broadcastTimer = new Timer(Configuration.AGENT_BROADCAST_TIMER)
    broadcastTimer.start()
    fireLocationInfoTimer = new Timer(500)
    fireLocationInfoTimer.start()
  }

  override def afterChangeRoom(): Unit = {

  }

  override def perform(delta: Int): Unit = {
    agent.broadcast(AgentLeading(agent, agent.shape.getCenterX, agent.shape.getCenterY), broadcastTimer)
    agent.broadcast(FireLocationInfo(agent.knownFireLocations), fireLocationInfoTimer)

    var door: Door = agent.findDoorToEnterNext()

    if (door != null) {
      agent.doorToEnter = door

      if (agent.controlScheme != ControlScheme.Manual) {
        agent.movementModule.moveTowards(door)

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

}


object LeaderBehavior {
  val name: String = "leader"
  val color: Color = Color.red
}