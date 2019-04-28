package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.util.ControlScheme
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

class AvoidFireBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  override def init(): Unit = {

  }

  override def perform(delta: Int): Unit = {
    if (agent.lastEntryDoor != null && agent.room.meetPointList.isEmpty) {

      agent.doorToEnter = agent.lastEntryDoor

      if (agent.controlScheme != ControlScheme.Manual) {
        val normalVector = new Vector2f(agent.lastEntryDoor.shape.getCenterX - agent.shape.getCenterX, agent.lastEntryDoor.shape.getCenterY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.movementModule.walkAngle = normalVector.getTheta.floatValue()

        if (!agent.movementModule.beingPushed) {
          if (!agent.atDoor) agent.movementModule.moveTowards(agent.lastEntryDoor)
          else agent.movementModule.stopMoving()
        }

      }

    }
    else if (agent.room.meetPointList.nonEmpty) {
      agent.followModule.setFollow(agent.room.meetPointList.head.shape.getCenterX, agent.room.meetPointList.head.shape.getCenterY)

      agent.behaviorModule.setBehavior(IdleBehavior.name)
    }
  }

  override def follow(that: Agent, posX: Float, posY: Float, atDistance: Float): Unit = {

  }

  override def afterChangeRoom(): Unit = {
    agent.behaviorModule.setBehavior(LeaderBehavior.name)
  }
}


object AvoidFireBehavior {
  val name: String = "avoidFire"
  val color: Color = Color.red
}