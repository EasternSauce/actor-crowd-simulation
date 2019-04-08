package com.kamilkurp.behaviors

import com.kamilkurp.agent.{Agent, AgentLeading}
import com.kamilkurp.building.Door
import com.kamilkurp.utils.{Configuration, ControlScheme, Timer}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

class LeaderBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {

  val broadcastTimer: Timer = new Timer(Configuration.AGENT_BROADCAST_TIMER)
  val waitAtDoorTimer: Timer = new Timer(Configuration.WAIT_AT_DOOR_TIMER)
  broadcastTimer.start()

  override def init(): Unit = {

  }

  def perform(delta: Int): Unit = {

    if (broadcastTimer.timedOut()) {
      agent.room.agentList.foreach(that => {
        if (that != agent) {
          that.actor ! AgentLeading(agent, agent.shape.getCenterX, agent.shape.getCenterY)
        }
      })
      broadcastTimer.reset()
    }


    var door: Door = null

    door = agent.findDoorToEnterNext()

    if (door != null) {
      agent.doorToEnter = door

      if (agent.controlScheme != ControlScheme.Manual) {
        val normalVector = new Vector2f(door.shape.getCenterX - agent.shape.getCenterX, door.shape.getCenterY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.walkAngle = normalVector.getTheta.floatValue()

        if (!agent.atDoor) {
          agent.currentVelocityX = normalVector.x * Configuration.AGENT_SPEED * (1f - agent.slow) * delta
          agent.currentVelocityY = normalVector.y * Configuration.AGENT_SPEED * (1f - agent.slow) * delta

          if (agent.getDistanceTo(agent.doorToEnter.shape.getCenterX, agent.doorToEnter.shape.getCenterY) < 100) {
            waitAtDoorTimer.reset()
            waitAtDoorTimer.start()
            agent.atDoor = true
          }
        }
        else {
          if (!waitAtDoorTimer.timedOut()) {
            agent.currentVelocityX = 0
            agent.currentVelocityY = 0
          }
          else {
            agent.currentVelocityX = normalVector.x * Configuration.AGENT_SPEED * (1f - agent.slow) * delta
            agent.currentVelocityY = normalVector.y * Configuration.AGENT_SPEED * (1f - agent.slow) * delta
          }
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

  }


}


object LeaderBehavior {
  val name: String = "leader"
  val color: Color = Color.red
}