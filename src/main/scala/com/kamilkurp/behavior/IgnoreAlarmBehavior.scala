package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.util.{Configuration, Timer}
import org.newdawn.slick.Color

import scala.util.Random

class IgnoreAlarmBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  var idleTimer: Timer = new Timer(Configuration.AGENT_IDLE_TIMER)
  var ignoreAlarmTimer: Timer = new Timer(10000 + Random.nextInt(4000))

  override def init(): Unit = {
    idleTimer.start()
    ignoreAlarmTimer.start()
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

        agent.changeBehavior(HoldMeetPointBehavior.name)
      }


    }
    if (ignoreAlarmTimer.timedOut()) {
      agent.changeBehavior(SearchExitBehavior.name)
    }
  }

  override def onChangeRoom(): Unit = {

  }

  override def onSpotFire(): Unit = {

  }
}


object IgnoreAlarmBehavior {
  val name: String = "ignoreAlarm"
  val color: Color = new Color(24, 100, 200)
}