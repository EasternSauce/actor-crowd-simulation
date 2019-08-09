package com.kamilkurp.behavior

import com.kamilkurp.agent.Agent
import com.kamilkurp.building.{Door, Room}
import com.kamilkurp.util.ControlScheme
import org.newdawn.slick.Color
import org.newdawn.slick.geom.Vector2f

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class SearchExitBehavior(agent: Agent, name: String, color: Color) extends Behavior(agent, name, color) {
  var doorToEnterNext: Door = _

  var visited: mutable.Set[Room] = _

  override def init(): Unit = {

    visited = mutable.Set[Room]()

    visited += agent.currentRoom

    decideOnDoor()

  }

  def decideOnDoor(): Unit = {
    var doorCandidate: Door = null

    var visitCandidates: ListBuffer[Door] = new ListBuffer[Door]()

    doorCandidate = agent.spatialModule.findDoorToEnterNext()


    val doorToCorrList: ListBuffer[Door] = new ListBuffer[Door]

    for (doorInRoom <- agent.currentRoom.doorList) {
      val leadingToRoom = doorInRoom.leadingToDoor.currentRoom

      if (leadingToRoom.name.startsWith("corr")) doorToCorrList += doorInRoom

      if (!agent.spatialModule.knownFireLocations.contains(leadingToRoom) && leadingToRoom.meetPointList.nonEmpty || (leadingToRoom.name.startsWith("corr") && !visited.contains(leadingToRoom))) {
        visitCandidates += doorInRoom
      }
    }

    if (doorCandidate != null) {
      doorToEnterNext = doorCandidate
    }
    else if (visitCandidates.nonEmpty){
      doorToEnterNext = visitCandidates(Random.nextInt(visitCandidates.size))
    }
    else {
      doorToEnterNext = doorToCorrList(Random.nextInt(doorToCorrList.length))
    }

  }

  def perform(delta: Int): Unit = {

    if (doorToEnterNext != null && agent.currentRoom.meetPointList.isEmpty) {
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
    visited += agent.currentRoom

    decideOnDoor()

  }

  override def onSpotFire(): Unit = {
    decideOnDoor()
  }

}


object SearchExitBehavior {
  val name: String = "searchExit"
  val color: Color = Color.orange
}