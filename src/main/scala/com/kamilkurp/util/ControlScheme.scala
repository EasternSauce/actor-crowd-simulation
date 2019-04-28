package com.kamilkurp.util

import akka.actor.{ActorSystem, Props}
import com.kamilkurp.agent.{Agent, AgentActor}
import com.kamilkurp.building.Room
import com.kamilkurp.simulation.CameraView
import org.jgrapht.Graph
import org.jgrapht.graph.{DefaultEdge, SimpleGraph}
import org.newdawn.slick.{GameContainer, Image, Input}
import org.newdawn.slick.geom.Vector2f

import scala.collection.mutable.ListBuffer

object ControlScheme extends Enumeration {
  type ControlScheme = Value
  val Manual, Static, Agent = Value

  def handleManualControls(agent: Agent, gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    var moved = false

    if (gc.getInput.isKeyDown(agent.controls._1)) {
      agent.movementModule.currentVelocityX = -Configuration.AGENT_SPEED
      moved = true
    }
    else if (gc.getInput.isKeyDown(agent.controls._2)) {
      agent.movementModule.currentVelocityX = Configuration.AGENT_SPEED
      moved = true
    }
    else {
      agent.movementModule.currentVelocityX = 0
    }
    if (gc.getInput.isKeyDown(agent.controls._3)) {
      agent.movementModule.currentVelocityY = -Configuration.AGENT_SPEED
      moved = true
    }
    else if (gc.getInput.isKeyDown(agent.controls._4)) {
      agent.movementModule.currentVelocityY = Configuration.AGENT_SPEED
      moved = true
    }
    else {
      agent.movementModule.currentVelocityY = 0
    }

    if (agent.movementModule.currentVelocityX != 0 || agent.movementModule.currentVelocityY != 0) {
      val normalVector = new Vector2f(agent.movementModule.currentVelocityX, agent.movementModule.currentVelocityY)
      normalVector.normalise()

      agent.movementModule.walkAngle = normalVector.getTheta.floatValue()
    }

    if (moved) {
      CameraView.x = agent.room.x + agent.shape.getX - Globals.WINDOW_X / renderScale / 2 + agent.shape.getWidth / 2
      CameraView.y = agent.room.y + agent.shape.getY - Globals.WINDOW_Y / renderScale / 2 + agent.shape.getHeight / 2
    }
  }

  def tryAddManualAgent(roomList: ListBuffer[Room], actorSystem: ActorSystem, agentImage: Image, roomGraph: SimpleGraph[Room, DefaultEdge]): Unit = {
    val roomsFiltered = roomList.filter(room => room.name == "roomA")

    val room1 = if (roomsFiltered.nonEmpty) roomsFiltered.head else null

    if (Configuration.ADD_MANUAL_AGENT) {
      val agent = new Agent(Configuration.MANUAL_AGENT_NAME, room1, ControlScheme.Manual, agentImage, roomGraph)
      agent.setControls((Input.KEY_A, Input.KEY_D, Input.KEY_W, Input.KEY_S))
      agent.init()

      val actor = actorSystem.actorOf(Props(new AgentActor(Configuration.MANUAL_AGENT_NAME, agent)))

      agent.setActor(actor)

      room1.agentList += agent
    }
  }
}
