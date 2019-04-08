package com.kamilkurp.simulation

import akka.actor.{ActorRef, ActorSystem, Props}
import com.kamilkurp.agent.{Agent, AgentActor}
import com.kamilkurp.building.{Door, MeetPoint, Room}
import com.kamilkurp.utils.{Configuration, ControlScheme, Globals}
import org.jgrapht.Graph
import org.jgrapht.graph.{DefaultEdge, SimpleGraph}
import org.newdawn.slick._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Random

object CameraView {
  var x: Float = 0.0f
  var y: Float = 0.0f
}

class Simulation(gameName: String) extends BasicGame(gameName) {
  val system: ActorSystem = ActorSystem("crowd_sim_system")
  val numberOfAgents: Int = 250
  val addManualAgent: Boolean = false
  val nameIndices: mutable.Map[String, Int] = mutable.Map[String, Int]()
  val listOfNames = Array("Virgil", "Dominique", "Hermina",
    "Carolynn", "Adina", "Elida", "Classie", "Raymonde",
    "Lovie", "Theola", "Damion", "Petronila", "Corrinne",
    "Arica", "Alfonso", "Madalene", "Alvina", "Eliana", "Jarrod", "Thora")
  var doorImage: Image = _
  var agentImage: Image = _

  var roomList: ListBuffer[Room] = new ListBuffer[Room]
  var doorList: ListBuffer[Door] = new ListBuffer[Door]

  var officeList: ListBuffer[Room] = new ListBuffer[Room]

  var mutableActorList = new ListBuffer[ActorRef]()


  var actorList: List[ActorRef] = mutableActorList.toList

  var renderScale: Float = 1.5f

  val roomGraph: Graph[Room, DefaultEdge] = new SimpleGraph[Room, DefaultEdge](classOf[DefaultEdge])


  override def init(gc: GameContainer): Unit = {
    doorImage = new Image("door.png")
    agentImage = new Image("character.png")

    val filename = "building.txt"
    for (line <- Source.fromFile(filename).getLines) {
      if (!line.isEmpty) {
        val split: Array[String] = line.split(" ")

        if (split(0) == "room") {
          val room = new Room(split(1), split(2).toInt, split(3).toInt, split(4).toInt, split(5).toInt)
          roomList += room
          if (room.name.startsWith("room")) officeList += room

        }
        else if (split(0) == "door") {
          var room: Room = null
          var linkToDoor: Door = null

          if (split.length >= 6) {
            roomList.foreach(that => {
              if (that.name == split(5)) room = that
            })
          }
          if (split.length >= 7) {
            doorList.foreach(that => {
              if (that.name == split(6)) linkToDoor = that
            })
          }
          val door = new Door(split(1), room, split(3).toInt, split(4).toInt, doorImage)
          if (linkToDoor != null) door.connectWith(linkToDoor)
          if (split(2) == "1") {
            room.evacuationDoor = door
          }
          doorList += door
        }
        else if (split(0) == "meet") {
          var room: Room = null

          if (split.length >= 5) {
            roomList.foreach(that => {
              if (that.name == split(4)) room = that
            })
          }

          val meetPoint = new MeetPoint(split(1), room, split(2).toInt, split(3).toInt)
          room.meetPointList += meetPoint
        }
      }
    }

    for (room <- roomList) {
      roomGraph.addVertex(room)
    }

    for (door <- doorList) {
      if (!roomGraph.containsEdge(door.room, door.leadingToDoor.room)) {
        roomGraph.addEdge(door.room, door.leadingToDoor.room)
      }
    }

    val roomsFiltered = roomList.filter(room => room.name == "roomA")
    val room1 = if (roomsFiltered.nonEmpty) roomsFiltered.head else null

    for (name <- listOfNames) {
      nameIndices.put(name, 0)
    }


    for (_ <- 0 until numberOfAgents) {

      val randomOffice = Random.nextInt(officeList.length)
      val room: Room = officeList(randomOffice)

      val randomNameIndex = Random.nextInt(listOfNames.length)
      val randomName = listOfNames(randomNameIndex) + nameIndices(listOfNames(randomNameIndex))
      nameIndices.put(listOfNames(randomNameIndex), nameIndices(listOfNames(randomNameIndex)) + 1)

      val agent = new Agent(randomName, room, ControlScheme.Agent, agentImage, roomGraph)
      room.agentList += agent
      val actor = system.actorOf(Props(new AgentActor(randomName, agent)))
      mutableActorList += actor

      agent.setActor(actor)
    }


    if (addManualAgent) {
      val agent = new Agent(Configuration.MANUAL_AGENT_NAME, room1, ControlScheme.Manual, (Input.KEY_A, Input.KEY_D, Input.KEY_W, Input.KEY_S), agentImage, roomGraph)

      val actor = system.actorOf(Props(new AgentActor(Configuration.MANUAL_AGENT_NAME, agent)))

      agent.setActor(actor)

      room1.agentList += agent
    }
  }

  override def update(gc: GameContainer, i: Int): Unit = {

    if (gc.getInput.isKeyDown(Input.KEY_DOWN)) {
      CameraView.y = CameraView.y + (1.0f * i.toFloat)
    }
    if (gc.getInput.isKeyDown(Input.KEY_UP)) {
      CameraView.y = CameraView.y - (1.0f * i.toFloat)
    }
    if (gc.getInput.isKeyDown(Input.KEY_RIGHT)) {
      CameraView.x = CameraView.x + (1.0f * i.toFloat)
    }
    if (gc.getInput.isKeyDown(Input.KEY_LEFT)) {
      CameraView.x = CameraView.x - (1.0f * i.toFloat)
    }

    if (gc.getInput.isKeyDown(Input.KEY_SUBTRACT)) {
      val centerX = CameraView.x + Globals.WINDOW_X * 1 / renderScale / 2
      val centerY = CameraView.y + Globals.WINDOW_Y * 1 / renderScale / 2
      renderScale /= 1 + 0.005f
      CameraView.x = centerX - (Globals.WINDOW_X * 1 / renderScale / 2)
      CameraView.y = centerY - (Globals.WINDOW_Y * 1 / renderScale / 2)

    }
    if (gc.getInput.isKeyDown(Input.KEY_ADD)) {
      val centerX = CameraView.x + Globals.WINDOW_X * 1 / renderScale / 2
      val centerY = CameraView.y + Globals.WINDOW_Y * 1 / renderScale / 2
      renderScale *= 1 + 0.005f
      CameraView.x = centerX - (Globals.WINDOW_X * 1 / renderScale / 2)
      CameraView.y = centerY - (Globals.WINDOW_Y * 1 / renderScale / 2)
    }


    roomList.foreach(room => {
      room.update(gc, i, renderScale)
    })
  }


  override def render(gc: GameContainer, g: Graphics): Unit = {
    g.scale(renderScale, renderScale)
    roomList.foreach(room => {

      room.render(g, doorImage, CameraView.x, CameraView.y)
    })
  }



}
