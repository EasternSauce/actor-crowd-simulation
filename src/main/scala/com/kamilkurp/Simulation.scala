package com.kamilkurp

import akka.actor.{ActorRef, ActorSystem, Props}
import org.newdawn.slick._

import scala.collection.mutable.ListBuffer
import scala.util.Random

object CameraView {
  var x: Float = 0.0f
  var y: Float = 0.0f
}

class Simulation(gameName: String) extends BasicGame(gameName) {
  val system: ActorSystem = ActorSystem("crowd_sim_system")

  // load resources
  var listOfNames = Array("Virgil", "Dominique", "Hermina",
    "Carolynn", "Adina", "Elida", "Classie", "Raymonde",
    "Lovie", "Theola", "Damion", "Petronila", "Corrinne",
    "Arica", "Alfonso", "Madalene", "Alvina", "Eliana", "Jarrod", "Thora")

  var doorImage: Image = _
  var characterImage: Image = _

  var roomList: ListBuffer[Room] = new ListBuffer[Room]

  var mutableActorList = new ListBuffer[ActorRef]()


  var actorList: List[ActorRef] = mutableActorList.toList

  var timer = 0

  override def init(gc: GameContainer): Unit = {
    doorImage = new Image("door.png")
    characterImage = new Image("character.png")

    val room1 = new Room("Room A", 100, 100, 400, 900)
    val room2 = new Room("Room B", 100, 1100, 1200, 500)
    val room3 = new Room("Room C", 1400, 1100, 500, 1500)
    val room4 = new Room("Room D", 800, 2100, 500, 500)


    for (_ <- 1 to 20) {

      val randomNameIndex = Random.nextInt(listOfNames.length)
      val randomName = listOfNames(randomNameIndex)
      listOfNames = listOfNames.take(randomNameIndex) ++ listOfNames.drop(randomNameIndex + 1)
      val character = new Character(randomName, room1, ControlScheme.Random, characterImage)
      room1.characterList += character
      val actor = system.actorOf(Props(new CharacterActor(randomName, character)))
      mutableActorList += actor

      character.setActor(actor)
    }

    val playerName = "Player"
    val character = new Character(playerName, room1, ControlScheme.Manual, (Input.KEY_A, Input.KEY_D, Input.KEY_W, Input.KEY_S), characterImage)
    room1.characterList += character

    val doorAB = new Door("Door AB", room1, 170, 845, doorImage)
    val doorBA = new Door("Door BA", room2, 180, 10, doorImage)
    doorAB.connectWith(doorBA)
    room1.evacuationDoor = doorAB

    val doorBC = new Door("Door BC", room2, 1160, 215, doorImage)
    val doorCB = new Door("Door CB", room3, 10, 215, doorImage)
    doorBC.connectWith(doorCB)
    room2.evacuationDoor = doorBC

    val doorCD = new Door("Door CD", room3, 10, 1215, doorImage)
    val doorDC = new Door("Door DC", room4, 460, 215, doorImage)
    doorCD.connectWith(doorDC)
    room3.evacuationDoor = doorCD

    roomList += (room1, room2, room3, room4)
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

    roomList.foreach(room => {
      room.update(gc, i)
    })
  }


  override def render(gc: GameContainer, g: Graphics): Unit = {
    roomList.foreach(room => {
      room.render(g, doorImage, CameraView.x, CameraView.y)
    })
  }
}
