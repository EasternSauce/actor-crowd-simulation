package com.kamilkurp.behavior_utils

import com.kamilkurp.agent.{Agent, AgentLeading}
import com.kamilkurp.behavior.Behavior
import com.kamilkurp.util.{Configuration, Timer}

trait Broadcasting {
  this: Behavior =>


  val broadcastTimer: Timer = new Timer(Configuration.AGENT_BROADCAST_TIMER)

  def broadcastingInit(): Unit = {
    broadcastTimer.start()
  }

  def broadcastLeading(): Unit = {
    if (broadcastTimer.timedOut()) {
      agent.room.agentList.foreach(that => {
        if (that != agent) {
          that.actor ! AgentLeading(agent, agent.shape.getCenterX, agent.shape.getCenterY)
        }
      })
      broadcastTimer.reset()
    }
  }
}
