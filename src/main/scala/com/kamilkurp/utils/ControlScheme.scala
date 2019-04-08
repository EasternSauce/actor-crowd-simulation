package com.kamilkurp.utils

import com.kamilkurp.agent.Agent
import com.kamilkurp.simulation.CameraView
import org.newdawn.slick.GameContainer
import org.newdawn.slick.geom.Vector2f

object ControlScheme extends Enumeration {
  type ControlScheme = Value
  val Manual, Static, Agent = Value

  def handleManualControls(agent: Agent, gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    var moved = false

    if (gc.getInput.isKeyDown(agent.controls._1)) {
      agent.currentVelocityX = -Configuration.AGENT_SPEED * delta
      moved = true
    }
    else if (gc.getInput.isKeyDown(agent.controls._2)) {
      agent.currentVelocityX = Configuration.AGENT_SPEED * delta
      moved = true
    }
    else {
      agent.currentVelocityX = 0
    }
    if (gc.getInput.isKeyDown(agent.controls._3)) {
      agent.currentVelocityY = -Configuration.AGENT_SPEED * delta
      moved = true
    }
    else if (gc.getInput.isKeyDown(agent.controls._4)) {
      agent.currentVelocityY = Configuration.AGENT_SPEED * delta
      moved = true
    }
    else {
      agent.currentVelocityY = 0
    }

    if (agent.currentVelocityX != 0 || agent.currentVelocityY != 0) {
      val normalVector = new Vector2f(agent.currentVelocityX, agent.currentVelocityY)
      normalVector.normalise()

      agent.walkAngle = normalVector.getTheta.floatValue()
    }

    if (moved) {
      CameraView.x = agent.room.x + agent.shape.getX - Globals.WINDOW_X / renderScale / 2 + agent.shape.getWidth / 2
      CameraView.y = agent.room.y + agent.shape.getY - Globals.WINDOW_Y / renderScale / 2 + agent.shape.getHeight / 2
    }
  }
}
