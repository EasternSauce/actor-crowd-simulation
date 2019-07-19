package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.building.Door
import com.kamilkurp.util.ControlScheme
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class PanicBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  var doorToEnterNext: Door = _
  var destinationX: Int = _
  var destinationY: Int = _
  var reachedDestination: Boolean = _

  override def init(): Unit = {

    decideDestination()

  }

  def decideDestination(): Unit = {
    doorToEnterNext = agent.currentRoom.doorList(Random.nextInt(agent.currentRoom.doorList.length))
    reachedDestination = false
    destinationX = Random.nextInt(agent.currentRoom.w)
    destinationY = Random.nextInt(agent.currentRoom.h)
  }

  def perform(delta: Int): Unit = {

    if (!reachedDestination) {
      agent.movementModule.moveTowards(destinationX, destinationY)

      if (agent.getDistanceTo(destinationX, destinationY) > 200) {
        agent.movementModule.moveTowards(destinationX, destinationY)
      }
      else {
        reachedDestination = true
      }
    }
    else if (doorToEnterNext != null && agent.currentRoom.meetPointList.isEmpty) {
      agent.intendedDoor = doorToEnterNext

      if (agent.controlScheme != ControlScheme.Manual) {
        val normalVector = new Vector2f(doorToEnterNext.shape.getCenterX - agent.shape.getCenterX, doorToEnterNext.shape.getCenterY - agent.shape.getCenterY)
        normalVector.normalise()

        agent.movementModule.walkAngle = normalVector.getTheta.floatValue()

        if (!agent.movementModule.beingPushed) {
          agent.movementModule.moveTowards(doorToEnterNext)
        }

      }

    }
    else if (agent.currentRoom.meetPointList.nonEmpty) {
      agent.followX = agent.currentRoom.meetPointList.head.shape.getCenterX
      agent.followY = agent.currentRoom.meetPointList.head.shape.getCenterY


      agent.changeBehavior(IdleBehavior.name)
    }
  }

  override def onChangeRoom(): Unit = {
    decideDestination()
  }

  override def onSpotFire(): Unit = {
    decideDestination()
  }

}


object PanicBehavior {
  val name: String = "panic"
  val color: Color = Color.red
}