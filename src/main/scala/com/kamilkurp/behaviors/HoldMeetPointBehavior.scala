package com.kamilkurp.behaviors

import com.kamilkurp.agent.Agent
import com.kamilkurp.utils.Timer
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class HoldMeetPointBehavior(agent: Agent) extends Behavior(agent) {
  val deviationTimer: Timer = new Timer(500)
  var deviationX: Float = 0
  var deviationY: Float = 0

  override def init(): Unit = {

  }

  override def perform(delta: Int): Unit = {
    val normalVector = new Vector2f(agent.followX - agent.shape.getCenterX, agent.followY - agent.shape.getCenterY)
    normalVector.normalise()

    if (deviationTimer.timedOut()) {
      deviationX = 0.3f * Random.nextFloat() - 0.15f
      deviationY = 0.3f * Random.nextFloat() - 0.15f
      deviationTimer.reset()
    }
    agent.currentVelocityX = (normalVector.x + deviationX) * agent.speed * (1f - agent.slow) * delta
    agent.currentVelocityY = (normalVector.y + deviationY) * agent.speed * (1f - agent.slow) * delta
  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {

  }

  override def afterChangeRoom(): Unit = {

  }
}
