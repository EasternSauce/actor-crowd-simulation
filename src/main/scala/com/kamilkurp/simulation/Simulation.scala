package com.kamilkurp.simulation

import java.io.FileWriter

import akka.actor.{ActorRef, ActorSystem, Props}
import com.kamilkurp.agent.{Agent, AgentActor}
import com.kamilkurp.behavior.{IdleBehavior, IgnoreAlarmBehavior, SearchExitBehavior}
import com.kamilkurp.building.{Door, Floor, MeetPoint, Room}
import com.kamilkurp.flame.FlamesManager
import com.kamilkurp.stats.Statistics
import com.kamilkurp.stats.Statistics.params
import com.kamilkurp.util._
import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge}
import org.newdawn.slick._
import org.newdawn.slick.geom.Rectangle
import org.newdawn.slick.gui.TextField
import org.newdawn.slick.opengl.SlickCallable
import org.newdawn.slick.opengl.renderer.Renderer

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
  var stairsImage: Image = _

  var roomList: ListBuffer[Room] = _
  var officeList: ListBuffer[Room] = _
  var agentList: ListBuffer[Agent] = _
  var actorList: ListBuffer[ActorRef] = _
  var floorList: ListBuffer[Floor] = _

  var currentFloor: Int = _

  var flamesManager: FlamesManager = _

  var roomGraph: DefaultDirectedWeightedGraph[Room, DefaultWeightedEdge] = _

  var textField: TextField = _

  var font: Font = _

  var currentMonitored: String = _
  var textFieldFocused: Boolean = _

  var untilAlarmTimer: Timer = _

  var manualControlsManager: CameraControls = _

  var cameraControls: CameraControls = _

  var generalTimer: Timer = _

  var selectedAgent: Agent = _

  var mainMenu: MainMenu = _

  var autoMode: Boolean = _

  var currentSimulation: Int = _

  override def init(gc: GameContainer): Unit = {
    gc.setAlwaysRender(true)
    gc.setUpdateOnlyWhenVisible(false)

    cameraControls = new CameraControls()

    mainMenu = MainMenu(gc, cameraControls.renderScale, this)
    mainMenu.onConfirm()

    autoMode = false

    currentSimulation = 0


    font = new TrueTypeFont(new java.awt.Font("Verdana", java.awt.Font.BOLD, (12 * 1 / cameraControls.renderScale).toInt), false)
    textField = new TextField(gc, font, 0, (Globals.WINDOW_Y * 0.955f).toInt, Globals.WINDOW_X, (Globals.WINDOW_Y * 0.04f).toInt)
    textField.setBorderColor(Color.transparent)
    textField.setTextColor(Color.green)

  }

  def setup(): Unit = {
    doorImage = new Image(Configuration.DOOR_IMAGE_LOCATION)
    agentImage = new Image(Configuration.AGENT_IMAGE_LOCATION)
    stairsImage = new Image(Configuration.STAIRS_IMAGE_LOCATION)

    actorSystem = ActorSystem("crowd_sim_system")
    nameIndices = mutable.Map[String, Int]()
    roomList = new ListBuffer[Room]()
    officeList = new ListBuffer[Room]()
    agentList = new ListBuffer[Agent]()
    actorList = new ListBuffer[ActorRef]()
    floorList = new ListBuffer[Floor]()

    textFieldFocused = false

    roomGraph = new DefaultDirectedWeightedGraph[Room, DefaultWeightedEdge](classOf[DefaultWeightedEdge])

    listOfNames = Array("Virgil", "Dominique", "Hermina",
      "Carolynn", "Adina", "Elida", "Classie", "Raymonde",
      "Lovie", "Theola", "Damion", "Petronila", "Corrinne",
      "Arica", "Alfonso", "Madalene", "Alvina", "Eliana", "Jarrod", "Thora")


    loadBuildingPlan(Configuration.buildingPlanLocation)

    currentFloor = floorList.length - 1

    flamesManager = new FlamesManager()
    flamesManager.init(floorList)

    manualControlsManager = new CameraControls()

    untilAlarmTimer = new Timer(Configuration.untilAlarmTime)
    untilAlarmTimer.start()

    for (room <- roomList) {
      roomGraph.addVertex(room)
    }

    floorList.foreach(floor => floor.doorList.foreach(door => {
      if (!roomGraph.containsEdge(door.currentRoom, door.leadingToDoor.currentRoom)) {
        val edge1: DefaultWeightedEdge = roomGraph.addEdge(door.currentRoom, door.leadingToDoor.currentRoom)
        roomGraph.setEdgeWeight(edge1, 1.0f)
        val edge2: DefaultWeightedEdge = roomGraph.addEdge(door.leadingToDoor.currentRoom, door.currentRoom)
        roomGraph.setEdgeWeight(edge2, 1.0f)
      }
    }))


    for (name <- listOfNames) {
      nameIndices.put(name, 0)
    }


    for (_ <- 0 until Configuration.numberOfAgents) {
      val randomNameIndex = Random.nextInt(listOfNames.length)
      val randomName = listOfNames(randomNameIndex) + nameIndices(listOfNames(randomNameIndex))
      nameIndices.put(listOfNames(randomNameIndex), nameIndices(listOfNames(randomNameIndex)) + 1)
      val randomOffice = Random.nextInt(officeList.length)
      val room: Room = officeList(randomOffice)
      addAgent(randomName, room)
    }


    ControlScheme.tryAddManualAgent(roomList, actorSystem, agentImage, roomGraph)


    generalTimer = new Timer(0)
    generalTimer.start()

    selectedAgent = null
  }


  private def addAgent(name: String, room: Room): Agent = {

    val agent = Agent(name, room, ControlScheme.Autonomous, agentImage, roomGraph)
    room.agentList += agent
    val actor = actorSystem.actorOf(Props(new AgentActor(name, agent)))
    actorList += actor

    agentList += agent

    agent.setActor(actor)

    agent
  }

  private def loadBuildingPlan(fileName: String): Unit = {
    for (line <- Source.fromFile(fileName).getLines) {
      if (!line.isEmpty) {
        val split: Array[String] = line.split(" ")
        if (split(0) == "floor") {
          floorList += loadFloor(split(1), split(2))
        }
      }
    }

    for (line <- Source.fromFile(fileName).getLines) {
      if (!line.isEmpty) {
        val split: Array[String] = line.split(" ")
        if (split(0) == "stairs") {
          var room: Room = null
          var linkToDoor: Door = null

          var floor: Floor = floorList.filter(floor => floor.name == split(1)).head
          var targetFloor: Floor = null

          if (split.length >= 7) {
            floor.roomList.foreach(that => {
              if (that.name == split(6)) room = that
            })
          }
          if (split.length >= 9) {
            targetFloor = floorList.filter(floor => floor.name == split(7)).head
            targetFloor.doorList.foreach(that => {
              if (that.name == split(8)) {

                linkToDoor = that
              }
            })
          }
          val door = new Door(split(2), room, split(4).toInt, split(5).toInt, stairsImage)
          if (linkToDoor != null) {
            door.connectWith(linkToDoor)
          }
          if (split(3) == "1") {
            room.evacuationDoor = door
          }
          floor.doorList += door
        }
      }
    }
  }

  private def loadFloor(name: String, fileName: String): Floor = {
    val floor = Floor(name)

    for (line <- Source.fromFile(fileName).getLines) {
      if (!line.isEmpty) {
        val split: Array[String] = line.split(" ")

        if (split(0) == "room") {
          val room = new Room(split(1), split(2).toInt, split(3).toInt, split(4).toInt, split(5).toInt)
          roomList += room
          floor.roomList += room
          if (room.name.startsWith("room")) officeList += room

        }
        else if (split(0) == "door") {
          var room: Room = null
          var linkToDoor: Door = null

          if (split.length >= 6) {
            floor.roomList.foreach(that => {
              if (that.name == split(5)) room = that
            })
          }
          if (split.length >= 7) {
            floor.doorList.foreach(that => {
              if (that.name == split(6)) linkToDoor = that
            })
          }
          val door = new Door(split(1), room, split(3).toInt, split(4).toInt, doorImage)
          if (linkToDoor != null) door.connectWith(linkToDoor)
          if (split(2) == "1") {
            room.evacuationDoor = door
          }
          floor.doorList += door
        }
        else if (split(0) == "meet") {
          var room: Room = null

          if (split.length >= 5) {
            floor.roomList.foreach(that => {
              if (that.name == split(4)) room = that
            })
          }

          val meetPoint = new MeetPoint(split(1), room, split(2).toInt, split(3).toInt)
          room.meetPointList += meetPoint
        }
      }
    }

    floor
  }

  override def update(gc: GameContainer, i: Int): Unit = {

    if (currentSimulation >= 10) {
      System.exit(0)
    }

    if (generalTimer.time > 120000) {

      currentSimulation += 1

      val total = agentList.length.toString
      val evacuated = agentList.count(agent => agent.currentBehavior.name == "holdMeetPoint").toString
      val unconscious = agentList.count(agent => agent.unconscious).toString
      val panicking = agentList.count(agent => agent.currentBehavior.name == "panic").toString

      val fw = new FileWriter("output.txt", true)
      fw.write("\n" + Configuration.leaderPercentage.toString + "," + total + "," + evacuated + "," + unconscious + "," + panicking)
      fw.close()

      reset()
    }

    if (gc.getInput.isKeyPressed(Input.KEY_F3)) {
      reset()
    }

    if (gc.getInput.isKeyPressed(Input.KEY_G)) {
      Configuration.simulationSpeed /= 2.0f
    }

    if (gc.getInput.isKeyPressed(Input.KEY_H)) {
      Configuration.simulationSpeed *= 2.0f
      println(Configuration.simulationSpeed)
    }


    if (Screen.currentScreen == Screen.MainMenu) {
      mainMenu.update(gc, i)

      if (gc.getInput.isKeyPressed(Input.KEY_ENTER)) {
        Screen.currentScreen = Screen.Simulation
        mainMenu.onConfirm()
      }
    }
    else if (Screen.currentScreen == Screen.Simulation) {
      Timer.updateTimers(i)

      cameraControls.handleControls(gc, i)

      if (gc.getInput.isKeyPressed(Input.KEY_PRIOR)) {
        if (currentFloor < floorList.size - 1) currentFloor = currentFloor + 1
      }
      if (gc.getInput.isKeyPressed(Input.KEY_NEXT)) {
        if (currentFloor > 0) currentFloor = currentFloor - 1
      }

      floorList.foreach(floor => floor.roomList.foreach(room => {
        room.update(gc, i, cameraControls.renderScale)
      }))


      if (gc.getInput.isKeyPressed(Input.KEY_ENTER)) {
        if (textFieldFocused) {

          floorList.foreach(floor => floor.roomList.foreach(room => {
            room.agentList.filter(agent => agent.name == currentMonitored).foreach(agent => {
              agent.debug = false
            })
          }))

          currentMonitored = textField.getText

          floorList.foreach(floor => floor.roomList.foreach(room => {
            room.agentList.filter(agent => agent.name == currentMonitored).foreach(agent => {
              agent.debug = true
            })
          }))

          textField.setFocus(false)
          textFieldFocused = false
        }
        else {
          textField.setText("")
          textField.setFocus(true)
          textFieldFocused = true
        }
      }

      if (untilAlarmTimer.timedOut()) {
        untilAlarmTimer.stop()
        untilAlarmTimer.reset()

        agentList.foreach(agent => {
          if (agent.currentBehavior.name == IdleBehavior.name) {
            if (Random.nextFloat() < Configuration.IGNORE_ALARM_PERCENTAGE) {
              agent.changeBehavior(IgnoreAlarmBehavior.name)
            }
            else {
              agent.changeBehavior(SearchExitBehavior.name)
            }
          }
        })
      }

      flamesManager.handleFlamePropagation()

      val input = gc.getInput
      val xpos = input.getMouseX
      val ypos = input.getMouseY

      floorList.foreach(floor => floor.roomList.foreach(room => {
        val roomRect: Rectangle = new Rectangle(room.x, room.y, room.w, room.h)
        val point: Rectangle = new Rectangle(CameraView.x + 1 / cameraControls.renderScale * xpos, CameraView.y + 1 / cameraControls.renderScale * ypos, 5, 5)

        if (roomRect.intersects(point)) {

          for (agent <- room.agentList) {
            var agentRect = new Rectangle(room.x + agent.shape.getX, room.y + agent.shape.getY, agent.shape.getWidth, agent.shape.getHeight)

            if (agentRect.intersects(point)) {
              agent.mousedOver = true

              if (gc.getInput.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON)) {
                if (selectedAgent != null) selectedAgent.selected = false
                selectedAgent = agent
                agent.selected = true
              }
            }
          }
        }
      }))

      Statistics.params.put("Total agents", agentList.length.toString)
      Statistics.params.put("Total evacuated", agentList.count(agent => agent.currentBehavior.name == "holdMeetPoint").toString)
      Statistics.params.put("Total unconscious", agentList.count(agent => agent.unconscious).toString)
      Statistics.params.put("Total in panic", agentList.count(agent => agent.currentBehavior.name == "panic").toString)
      Statistics.params.put("Time", (generalTimer.time / 1000f).toString)


      if (selectedAgent != null) {
        params.put("Agent name", selectedAgent.name)
        params.put("Start room", selectedAgent.startingRoom.name)
        params.put("Velocity x", selectedAgent.movementModule.currentVelocityX.toString)
        params.put("Velocity y", selectedAgent.movementModule.currentVelocityY.toString)
        params.put("Behavior", selectedAgent.currentBehavior.name)
        params.put("Stress level", selectedAgent.stressLevel.toString)
        params.put("Stress resistance", selectedAgent.stressResistance.toString)
      }
    }
  }

  override def render(gc: GameContainer, g: Graphics): Unit = {

    if (Screen.currentScreen == Screen.MainMenu) {
      mainMenu.draw(gc, g)
    }
    else if (Screen.currentScreen == Screen.Simulation) {
      SlickCallable.enterSafeBlock()
      Renderer.get().glPushMatrix()

      g.scale(cameraControls.renderScale, cameraControls.renderScale)
      floorList(currentFloor).roomList.foreach(room => {

        room.render(g, doorImage, CameraView.x, CameraView.y)
      })
      g.setColor(Color.green)


      Renderer.get().glPopMatrix()
      SlickCallable.leaveSafeBlock()

      val textWindowX: Int = 0
      val textWindowY: Int = (Globals.WINDOW_Y * 0.7f).toInt

      g.setColor(new Color(0, 0, 0, 0.7f))
      g.fillRect(0, Globals.WINDOW_Y * 0.7f, Globals.WINDOW_X, Globals.WINDOW_Y * 0.25f)


      var i = 0
      for (param <- Statistics.params) {
        if (!param._2.equals("hide")) font.drawString(textWindowX + 20 + 200 * Math.floor(i / 8).toFloat, textWindowY + 20 + 18 * (i % 8), param._1 + ":", Color.green)
        if (!param._2.equals("hide")) font.drawString(textWindowX + 150 + 200 * Math.floor(i / 8).toFloat, textWindowY + 20 + 18 * (i % 8), param._2, Color.green)

        i = i + 1
      }

      g.setColor(Color.green)

      textField.render(gc, g)
    }

  }

  def reset(): Unit = {
    setup()

    //    agentList.foreach(agent => {
    //      agent.currentRoom.removeAgent(agent)
    //      agent.startingRoom.addAgent(agent)
    //
    //      agent.currentRoom = agent.startingRoom
    //      agent.shape.setX(agent.startingPosX)
    //      agent.shape.setY(agent.startingPosY)
    //    })
  }
}
