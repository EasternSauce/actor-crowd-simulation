package com.kamilkurp.behaviors

import com.kamilkurp.agent.Agent
import com.kamilkurp.utils.{Configuration, ControlScheme, Timer}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class IdleBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  var idleTimer: Timer = new Timer(Configuration.AGENT_IDLE_TIMER)



  override def init(): Unit = {
    idleTimer.start()
  }

  def perform(delta: Int): Unit = {
    if (idleTimer.timedOut()) {


      val inPlace = Random.nextInt(100) < 60

      idleTimer.reset()

      if (agent.controlScheme != ControlScheme.Manual) {
        if (inPlace) {
          agent.currentVelocityX = 0
          agent.currentVelocityY = 0
        }
        else {
          agent.currentVelocityX = (Random.nextInt(3) - 1) * Configuration.AGENT_SPEED * (1f - agent.slow) * 0.8f * delta
          agent.currentVelocityY = (Random.nextInt(3) - 1) * Configuration.AGENT_SPEED * (1f - agent.slow) * 0.8f * delta
        }
      }


      if (agent.currentVelocityX != 0 || agent.currentVelocityY != 0) {
        val normalVector = new Vector2f(agent.currentVelocityX, agent.currentVelocityY)
        normalVector.normalise()

        agent.walkAngle = normalVector.getTheta.floatValue()
      }

      if (agent.room.meetPointList.nonEmpty) {
        agent.followX = agent.room.meetPointList.head.shape.getCenterX
        agent.followY = agent.room.meetPointList.head.shape.getCenterY

        agent.setBehavior(HoldMeetPointBehavior.name)
      }


    }
  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {
    agent.setBehavior("follow")
    agent.followX = posX
    agent.followY = posY
    agent.followDistance = atDistance
    agent.followedAgent = that
    agent.followTimer.reset()
  }

  override def afterChangeRoom(): Unit = {

  }
}


object IdleBehavior {
  val name: String = "idle"
  val color: Color = Color.blue
}