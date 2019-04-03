package com.kamilkurp.behaviors

import com.kamilkurp.agent.{Agent, AgentLeading}
import com.kamilkurp.building.Door
import com.kamilkurp.utils.{ControlScheme, Timer}
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class LeaderBehavior(agent: Agent) extends Behavior(agent) {

  val deviationTimer: Timer = new Timer(500)
  val broadcastTimer: Timer = new Timer(300)

  val waitAtDoorTimer: Timer = new Timer(300)
  waitAtDoorTimer.time = waitAtDoorTimer.timeout

  var deviationX: Float = 0
  var deviationY: Float = 0

  override def init(): Unit = {

  }

  def perform(delta: Int): Unit = {
    deviationTimer.update(delta)
    broadcastTimer.update(delta)
    waitAtDoorTimer.update(delta)

    if (broadcastTimer.timedOut()) {
      agent.room.agentList.foreach(that => {
        if (that != agent) {
          that.actor ! AgentLeading(agent, agent.shape.getCenterX, agent.shape.getCenterY)
        }
      })
      broadcastTimer.reset()
    }


    val door: Door = agent.room.evacuationDoor
    if (door != null) {
      agent.doorToEnter = door
      if (deviationTimer.timedOut()) {
        deviationX = 0.3f * Random.nextFloat() - 0.15f
        deviationY = 0.3f * Random.nextFloat() - 0.15f
        deviationTimer.reset()
      }


      if (agent.controlScheme != ControlScheme.Manual) {
        val normalVector = new Vector2f(door.shape.getCenterX - agent.shape.getCenterX, door.shape.getCenterY - agent.shape.getCenterY)
        normalVector.normalise()

        if (!agent.atDoor) {
          agent.currentVelocityX = (normalVector.x + deviationX) * agent.speed * (1f - agent.slow) * delta
          agent.currentVelocityY = (normalVector.y + deviationY) * agent.speed * (1f - agent.slow) * delta

          if (agent.getDistanceTo(agent.doorToEnter.shape.getCenterX, agent.doorToEnter.shape.getCenterY) < 100) {
            waitAtDoorTimer.reset()
            agent.atDoor = true
          }
        }
        else {
          if (!waitAtDoorTimer.timedOut()) {
            agent.currentVelocityX = 0
            agent.currentVelocityY = 0
          }
          else {
            agent.currentVelocityX = (normalVector.x + deviationX) * agent.speed * (1f - agent.slow) * delta
            agent.currentVelocityY = (normalVector.y + deviationY) * agent.speed * (1f - agent.slow) * delta
          }
        }


      }

    }
    else if (agent.room.meetPointList.nonEmpty) {
      agent.followX = agent.room.meetPointList.head.shape.getCenterX
      agent.followY = agent.room.meetPointList.head.shape.getCenterY

      agent.setBehavior("holdMeetPoint")
    }
  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {

  }
}
