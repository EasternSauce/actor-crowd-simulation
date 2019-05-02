package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.util.{Configuration, Timer}
import org.newdawn.slick.Color

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

      if (inPlace) agent.movementModule.stopMoving()
      else agent.movementModule.moveTowards(agent.shape.getX + (Random.nextInt(3) - 1) * 50f, agent.shape.getY + (Random.nextInt(3) - 1) * 50f)

      if (agent.currentRoom.meetPointList.nonEmpty) {
        agent.followX = agent.currentRoom.meetPointList.head.shape.getCenterX
        agent.followY = agent.currentRoom.meetPointList.head.shape.getCenterY

        agent.setBehavior(HoldMeetPointBehavior.name)
      }


    }
  }

  override def afterChangeRoom(): Unit = {

  }
}


object IdleBehavior {
  val name: String = "idle"
  val color: Color = Color.blue
}