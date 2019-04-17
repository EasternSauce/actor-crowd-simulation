package com.kamilkurp.simulation

import akka.actor.{ActorRef, ActorSystem, Props}
import com.kamilkurp.agent.{Agent, AgentActor}
import com.kamilkurp.behavior.{IdleBehavior, SearchExitBehavior}
import com.kamilkurp.building.{Door, MeetPoint, Room}
import com.kamilkurp.entity.Flames
import com.kamilkurp.util.Globals.intersects
import com.kamilkurp.util.{Configuration, ControlScheme, Globals, Timer}
import org.jgrapht.Graph
import org.jgrapht.graph.{DefaultEdge, SimpleGraph}
import org.newdawn.slick._
import org.newdawn.slick.gui.TextField
import org.newdawn.slick.UnicodeFont

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
  val numberOfAgents: Int = Configuration.NUMBER_OF_AGENTS
  val addManualAgent: Boolean = false
  val nameIndices: mutable.Map[String, Int] = mutable.Map[String, Int]()
  val listOfNames = Array("Virgil", "Dominique", "Hermina",
    "Carolynn", "Adina", "Elida", "Classie", "Raymonde",
    "Lovie", "Theola", "Damion", "Petronila", "Corrinne",
    "Arica", "Alfonso", "Madalene", "Alvina", "Eliana", "Jarrod", "Thora")
  var doorImage: Image = _
  var agentImage: Image = _
  var flamesImage: Image = _

  var roomList: ListBuffer[Room] = new ListBuffer[Room]
  var doorList: ListBuffer[Door] = new ListBuffer[Door]

  var officeList: ListBuffer[Room] = new ListBuffer[Room]

  var agentList: ListBuffer[Agent] = new ListBuffer[Agent]

  var flamesList: ListBuffer[Flames] = new ListBuffer[Flames]


  var mutableActorList = new ListBuffer[ActorRef]()

  var actorList: List[ActorRef] = mutableActorList.toList

  var renderScale: Float = 1.5f

  val roomGraph: Graph[Room, DefaultEdge] = new SimpleGraph[Room, DefaultEdge](classOf[DefaultEdge])

  var textField: TextField = null

  var font: Font = null

  var currentMonitored: String = null
  var textFieldFocused: Boolean = false

  var untilAlarmTimer: Timer = new Timer(5000)
  untilAlarmTimer.start()


  var zoomTimer: Timer = new Timer(1000)
  zoomTimer.start()


  var flamesPropagationTimer: Timer = new Timer(500)
  flamesPropagationTimer.start()

  override def init(gc: GameContainer): Unit = {
    doorImage = new Image("door.png")
    agentImage = new Image("character.png")
    flamesImage = new Image("fire.png")

    gc.setAlwaysRender(true)
    gc.setUpdateOnlyWhenVisible(false)

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

      agentList += agent

      agent.setActor(actor)
    }


    if (addManualAgent) {
      val agent = new Agent(Configuration.MANUAL_AGENT_NAME, room1, ControlScheme.Manual, (Input.KEY_A, Input.KEY_D, Input.KEY_W, Input.KEY_S), agentImage, roomGraph)

      val actor = system.actorOf(Props(new AgentActor(Configuration.MANUAL_AGENT_NAME, agent)))

      agent.setActor(actor)

      room1.agentList += agent
    }

    val randomRoom = Random.nextInt(roomList.length)
    val room: Room = officeList(0)//roomList(randomRoom)

    val flames = new Flames(room, Random.nextInt(room.w-flamesImage.getWidth), Random.nextInt(room.h-flamesImage.getHeight), flamesImage)
    //val flames = new Flames(room, 50, 50, flamesImage)

    room.flamesList += flames
    flamesList += flames

    font = new TrueTypeFont(new java.awt.Font("Verdana", java.awt.Font.BOLD, (32*1/renderScale).toInt), false)
    textField = new TextField(gc, font, (20*1/renderScale).toInt, (20*1/renderScale).toInt, (360*1/renderScale).toInt, (70*1/renderScale).toInt)

  }

  override def update(gc: GameContainer, i: Int): Unit = {
    Timer.updateTimers(i)

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
        if (agent.currentBehavior == IdleBehavior.name) {
          agent.setBehavior(SearchExitBehavior.name)
        }
      })
    }

    if (flamesPropagationTimer.timedOut()) {
      flamesPropagationTimer.reset()

      handleFlamePropagation


    }

  }


  private def handleFlamePropagation = {

    val length = flamesList.length

    for (x <- 0 until length) {

      val flames = flamesList(x)

      if (!flames.dontUpdate) {



        var newFlames: Flames = null


        var pairs: ListBuffer[(Int, Int)] = new ListBuffer[(Int, Int)]()

        for (i <- -1 to 1) {
          for (j <- -1 to 1) {
            if (!(i == 0 && j == 0)) {
              pairs.append((i, j))
            }
          }
        }

        var shuffledPairs = Random.shuffle(pairs)


        var foundSpot = false
        for (pair <- shuffledPairs) {


          if (!foundSpot) {


            newFlames = new Flames(flames.room, flames.shape.getX, flames.shape.getY, flames.image)

            newFlames.shape.setX(flames.shape.getX.toInt + (flamesImage.getWidth + 5) * pair._1)
            newFlames.shape.setY(flames.shape.getY.toInt + (flamesImage.getHeight + 5) * pair._2)

            var isFree = true

            newFlames.room.flamesList.foreach(that => {
              if (newFlames.shape.getX < 0 || newFlames.shape.getX > newFlames.room.w - newFlames.shape.getWidth) isFree = false
              if (newFlames.shape.getY < 0 || newFlames.shape.getY > newFlames.room.h - newFlames.shape.getHeight) isFree = false

              if (Globals.intersects(newFlames, that.shape.getX, that.shape.getY, that.shape.getWidth, that.shape.getHeight, 0, 0)) {
                isFree = false
              }
            })

            if (isFree) {
              flames.room.flamesList += newFlames
              flamesList += newFlames

              foundSpot = true

              newFlames.room.doorList.foreach(that => {
                if (Globals.intersects(newFlames, that.shape.getX, that.shape.getY, that.shape.getWidth, that.shape.getHeight, 0, 0)) {
                  var foundNewRoomSpot = false
                  for (i <- -1 to 1) {
                    for (j <- -1 to 1) {
                      if (!foundNewRoomSpot) {
                        val leadingToDoor = that.leadingToDoor
                        val spotX = leadingToDoor.posX + i * 60
                        val spotY = leadingToDoor.posY + j * 60

                        if (!Globals.isRectOccupied(leadingToDoor.room, spotX - 10, spotY - 10, newFlames.shape.getWidth + 20, newFlames.shape.getHeight + 20)) {

                          val newRoom: Room = that.leadingToDoor.room

                          val newRoomFlames =  new Flames(newRoom, spotX, spotY, flamesImage)
                          newRoom.flamesList += newRoomFlames
                          flamesList += newRoomFlames

                          foundNewRoomSpot = true
                        }
                      }

                    }
                  }
                }
              })

            }
          }



        }

        if(!foundSpot) {
          flames.dontUpdate = true
        }


      }
    }
  }



  override def render(gc: GameContainer, g: Graphics): Unit = {
    g.scale(renderScale, renderScale)
    roomList.foreach(room => {

      room.render(g, doorImage, CameraView.x, CameraView.y)
    })
    g.setColor(Color.green)

    textField.render(gc, g)
  }

}
