package com.kamilkurp.simulation

import akka.actor.{ActorRef, ActorSystem, Props}
import com.kamilkurp.agent.{Agent, AgentActor}
import com.kamilkurp.behavior.{IdleBehavior, SearchExitBehavior}
import com.kamilkurp.building.{Door, MeetPoint, Room}
import com.kamilkurp.flame.FlamesManager
import com.kamilkurp.util._
import org.jgrapht.Graph
import org.jgrapht.graph.{DefaultEdge, SimpleGraph}
import org.newdawn.slick._
import org.newdawn.slick.gui.TextField

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Random


object CameraView {
  var x: Float = 0.0f
  var y: Float = 0.0f
}

class Simulation(gameName: String) extends BasicGame(gameName) {
  var actorSystem: ActorSystem = _
  var nameIndices: mutable.Map[String, Int] = _
  var listOfNames: Array[String] = _
  var doorImage: Image = _
  var agentImage: Image = _

  var roomList: ListBuffer[Room] = _
  var doorList: ListBuffer[Door] = _
  var officeList: ListBuffer[Room] = _
  var agentList: ListBuffer[Agent] = _
  var actorList: ListBuffer[ActorRef] = _

  var flamesManager: FlamesManager = _

  var roomGraph: Graph[Room, DefaultEdge] = _

  var textField: TextField = _

  var font: Font = _

  var currentMonitored: String = _
  var textFieldFocused: Boolean = _

  var untilAlarmTimer: Timer = _

  var zoomTimer: Timer = _

  var manualControlsManager: CameraControls = _

  var cameraControls: CameraControls = _



  override def init(gc: GameContainer): Unit = {
    doorImage = new Image(Configuration.DOOR_IMAGE_LOCATION)
    agentImage = new Image(Configuration.AGENT_IMAGE_LOCATION)

    actorSystem = ActorSystem("crowd_sim_system")
    nameIndices = mutable.Map[String, Int]()
    roomList = new ListBuffer[Room]()
    doorList = new ListBuffer[Door]()
    officeList = new ListBuffer[Room]()
    agentList = new ListBuffer[Agent]()
    actorList = new ListBuffer[ActorRef]()

    textFieldFocused = false

    roomGraph = new SimpleGraph[Room, DefaultEdge](classOf[DefaultEdge])

    listOfNames = Array("Virgil", "Dominique", "Hermina",
      "Carolynn", "Adina", "Elida", "Classie", "Raymonde",
      "Lovie", "Theola", "Damion", "Petronila", "Corrinne",
      "Arica", "Alfonso", "Madalene", "Alvina", "Eliana", "Jarrod", "Thora")

    gc.setAlwaysRender(true)
    gc.setUpdateOnlyWhenVisible(false)

    loadBuildingPlan()

    flamesManager = new FlamesManager()
    flamesManager.init(roomList)

    manualControlsManager = new CameraControls()

    untilAlarmTimer = new Timer(Configuration.UNTIL_ALARM_TIME)
    untilAlarmTimer.start()

    zoomTimer = new Timer(1000)
    zoomTimer.start()

    for (room <- roomList) {
      roomGraph.addVertex(room)
    }

    for (door <- doorList) {
      if (!roomGraph.containsEdge(door.room, door.leadingToDoor.room)) {
        roomGraph.addEdge(door.room, door.leadingToDoor.room)
      }
    }

    for (name <- listOfNames) {
      nameIndices.put(name, 0)
    }


    for (_ <- 0 until Configuration.NUMBER_OF_AGENTS) {

      val randomOffice = Random.nextInt(officeList.length)
      val room: Room = officeList(randomOffice)

      val randomNameIndex = Random.nextInt(listOfNames.length)
      val randomName = listOfNames(randomNameIndex) + nameIndices(listOfNames(randomNameIndex))
      nameIndices.put(listOfNames(randomNameIndex), nameIndices(listOfNames(randomNameIndex)) + 1)

      val agent = new Agent(randomName, room, ControlScheme.Agent, agentImage, roomGraph)
      agent.init()
      room.agentList += agent
      val actor = actorSystem.actorOf(Props(new AgentActor(randomName, agent)))
      actorList += actor

      agentList += agent

      agent.setActor(actor)
    }

    ControlScheme.tryAddManualAgent(roomList, actorSystem, agentImage, roomGraph)

    cameraControls = new CameraControls()
    font = new TrueTypeFont(new java.awt.Font("Verdana", java.awt.Font.BOLD, (32*1/cameraControls.renderScale).toInt), false)
    textField = new TextField(gc, font, (20*1/cameraControls.renderScale).toInt, (20*1/cameraControls.renderScale).toInt, (360*1/cameraControls.renderScale).toInt, (70*1/cameraControls.renderScale).toInt)

  }



  override def update(gc: GameContainer, i: Int): Unit = {
    Timer.updateTimers(i)

    cameraControls.handleControls(gc, i)


    roomList.foreach(room => {
      room.update(gc, i, cameraControls.renderScale)
    })


    if (gc.getInput.isKeyPressed(Input.KEY_ENTER)) {
      if (textFieldFocused) {

        roomList.foreach(room => {
          room.agentList.filter(agent => agent.name == currentMonitored).foreach(agent => {
            agent.debug = false
          })
        })

        currentMonitored = textField.getText

        roomList.foreach(room => {
          room.agentList.filter(agent => agent.name == currentMonitored).foreach(agent => {
            agent.debug = true
          })
        })

        textField.setFocus(false)
        textFieldFocused = false
      }
      else {
        textField.setText("")
        textField.setFocus(true)
        textFieldFocused = true
      }


    }

    roomList.foreach(room => {
      room.agentList.filter(agent => agent.name == currentMonitored).foreach(agent => {
        println("current_velocity_x=" + agent.currentVelocityX)
        println("current_velocity_y=" + agent.currentVelocityY)
        println("being_pushed=" + agent.beingPushed)
        println("pushed_timer=" + agent.pushedTimer.time)
      })
    })

    if (untilAlarmTimer.timedOut()) {
      untilAlarmTimer.stop()
      untilAlarmTimer.reset()

      agentList.foreach(agent => {
        if (agent.behaviorManager.currentBehavior == IdleBehavior.name) {
          agent.behaviorManager.setBehavior(SearchExitBehavior.name)
        }
      })
    }

    flamesManager.handleFlamePropagation()

  }


  override def render(gc: GameContainer, g: Graphics): Unit = {
    g.scale(cameraControls.renderScale, cameraControls.renderScale)
    roomList.foreach(room => {

      room.render(g, doorImage, CameraView.x, CameraView.y)
    })
    g.setColor(Color.green)

    textField.render(gc, g)
  }

  private def loadBuildingPlan(): Unit = {
    val filename = Configuration.BUILDING_PLAN_LOCATION
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
  }
}
