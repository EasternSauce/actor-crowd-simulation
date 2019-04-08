package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.util.{Configuration, ControlScheme, Timer}
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

      if (inPlace) agent.stopMoving()
      else agent.moveTowards(agent.shape.getX + (Random.nextInt(3) - 1) * 50f, agent.shape.getY + (Random.nextInt(3) - 1) * 50f, delta)

      if (agent.room.meetPointList.nonEmpty) {
        agent.setFollow(agent.room.meetPointList.head.shape.getCenterX, agent.room.meetPointList.head.shape.getCenterY)

        agent.setBehavior(HoldMeetPointBehavior.name)
      }


    }
  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {
    agent.setBehavior(FollowBehavior.name)

    agent.setFollow(posX, posY)
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