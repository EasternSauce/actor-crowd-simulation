package com.kamilkurp.behaviors

import com.kamilkurp.agent.Agent
import com.kamilkurp.utils.{ControlScheme, Timer}
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class IdleBehavior(agent: Agent) extends Behavior(agent) {
  var idleTimer: Timer = new Timer(500)

  override def init(): Unit = {

  }

  def perform(delta: Int): Unit = {
    idleTimer.update(delta)

    if (idleTimer.timedOut()) {
      val inPlace = Random.nextInt(100) < 60

      idleTimer.reset()

      if (agent.controlScheme != ControlScheme.Manual) {
        if (inPlace) {
          agent.currentVelocityX = 0
          agent.currentVelocityY = 0
        }
        else {
          agent.currentVelocityX = (Random.nextInt(3) - 1) * agent.speed * (1f - agent.slow) * 0.8f * delta
          agent.currentVelocityY = (Random.nextInt(3) - 1) * agent.speed * (1f - agent.slow) * 0.8f * delta
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

        agent.setBehavior("holdMeetPoint")
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
}
