package com.kamilkurp.simulation

import akka.actor.{ActorRef, ActorSystem, Props}
import com.kamilkurp.agent.{Agent, AgentActor}
import com.kamilkurp.behavior.{IdleBehavior, LeaderBehavior, SearchExitBehavior, StationaryBehavior}
import com.kamilkurp.building.{Door, MeetPoint, Room}
import com.kamilkurp.flame.FlamesManager
import com.kamilkurp.stats.Statistics
import com.kamilkurp.util._
import org.jgrapht.Graph
import org.jgrapht.graph.{DefaultEdge, SimpleGraph}
import org.newdawn.slick._
import org.newdawn.slick.gui.TextField
import org.newdawn.slick.opengl.renderer.Renderer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Random
import org.newdawn.slick.opengl.SlickCallable



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

  var roomGraph: SimpleGraph[Room, DefaultEdge] = _

  var textField: TextField = _

  var font: Font = _

  var currentMonitored: String = _
  var textFieldFocused: Boolean = _

  var untilAlarmTimer: Timer = _

  var zoomTimer: Timer = _

  var manualControlsManager: CameraControls = _

  var cameraControls: CameraControls = _

  var generalTimer: Timer = _

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
      val randomNameIndex = Random.nextInt(listOfNames.length)
      val randomName = listOfNames(randomNameIndex) + nameIndices(listOfNames(randomNameIndex))
      //println("adding " + randomName)
      nameIndices.put(listOfNames(randomNameIndex), nameIndices(listOfNames(randomNameIndex)) + 1)
      val randomOffice = Random.nextInt(officeList.length)
      val room: Room = officeList(randomOffice)


      addAgent(randomName, room)
    }

//    val agent1 = addAgent("agent1", officeList.head)
//    agent1.shape.setCenterX(360)
//    agent1.shape.setCenterY(300)
//    agent1.behaviorManager.setBehavior(StationaryBehavior.name)
//
//    val agent2 = addAgent("agent2", officeList.head)
//    agent2.shape.setCenterX(450)
//    agent2.shape.setCenterY(280)
//    agent2.behaviorManager.setBehavior(StationaryBehavior.name)
//
//    val agent3 = addAgent("agent3", officeList.head)
//    agent3.shape.setCenterX(400)
//    agent3.shape.setCenterY(320)
//    agent3.behaviorManager.setBehavior(StationaryBehavior.name)
//
//    val agent4 = addAgent("agent4", officeList.head)
//    agent4.shape.setCenterX(380)
//    agent4.shape.setCenterY(270)
//    agent4.behaviorManager.setBehavior(StationaryBehavior.name)
//
//    val agent5 = addAgent("agent5", officeList.head)
//    agent5.shape.setCenterX(320)
//    agent5.shape.setCenterY(290)
//    agent5.behaviorManager.setBehavior(StationaryBehavior.name)
//
//    val agent6 = addAgent("agent6", officeList.head)
//    agent6.shape.setCenterX(50)
//    agent6.shape.setCenterY(150)
//    agent6.behaviorManager.setBehavior(LeaderBehavior.name)
//    agent6.viewCone.drawRays = true

    ControlScheme.tryAddManualAgent(roomList, actorSystem, agentImage, roomGraph)

    cameraControls = new CameraControls()
    font = new TrueTypeFont(new java.awt.Font("Verdana", java.awt.Font.BOLD, (32*1/cameraControls.renderScale).toInt), false)
    textField = new TextField(gc, font, 0, (Globals.WINDOW_Y * 0.955f).toInt, Globals.WINDOW_X, (Globals.WINDOW_Y * 0.04f).toInt)
    textField.setBorderColor(Color.transparent)
    textField.setTextColor(Color.green)

    generalTimer = new Timer(0)
    generalTimer.start()
  }


  private def addAgent(name: String, room: Room): Agent = {

    val agent = new Agent(name, room, ControlScheme.Agent, agentImage, roomGraph)
    agent.init()
    room.agentList += agent
    val actor = actorSystem.actorOf(Props(new AgentActor(name, agent)))
    actorList += actor

    agentList += agent

    agent.setActor(actor)

    agent
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

//    roomList.foreach(room => {
//      room.agentList.filter(agent => agent.name == currentMonitored).foreach(agent => {
//        Statistics.params.put("doorToEnter", agent.doorToEnter.name)
//      })
//    })

    if (untilAlarmTimer.timedOut()) {
      untilAlarmTimer.stop()
      untilAlarmTimer.reset()

      agentList.foreach(agent => {
        if (agent.currentBehavior.name == IdleBehavior.name) {
          agent.setBehavior(SearchExitBehavior.name)
        }
      })
    }

    flamesManager.handleFlamePropagation()


    Statistics.params.put("Total agents", agentList.length.toString)
    Statistics.params.put("Total evacuated", agentList.filter(agent => agent.currentBehavior.name == "holdMeetPoint").toList.length.toString)
    Statistics.params.put("Time", (generalTimer.time/1000f).toString)

  }

  override def render(gc: GameContainer, g: Graphics): Unit = {

    SlickCallable.enterSafeBlock()
    Renderer.get().glPushMatrix()

    g.scale(cameraControls.renderScale, cameraControls.renderScale)
    roomList.foreach(room => {

      room.render(g, doorImage, CameraView.x, CameraView.y)
    })
    g.setColor(Color.green)


    Renderer.get().glPopMatrix()
    SlickCallable.leaveSafeBlock()

    val textWindowX: Int = 0
    val textWindowY: Int = (Globals.WINDOW_Y * 0.7f).toInt

    g.setColor(new Color(0,0,0,0.7f))
    g.fillRect(0, Globals.WINDOW_Y * 0.7f, Globals.WINDOW_X, Globals.WINDOW_Y * 0.25f)


    var i = 0
    for (param <- Statistics.params) {
      font.drawString(textWindowX + 20 + 350 * Math.floor(i/8).toFloat, textWindowY + 20 + 40 * (i%8), param._1 + ": " + param._2, Color.green)
      i = i + 1
    }

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
