package com.kamilkurp

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.util.Random
import org.newdawn.slick._


import scala.language.postfixOps
import scala.collection.mutable.ListBuffer


object Globals {
  val CHARACTER_SIZE: Int = 40
}

object Greeter {
  def props(message: String, printerActor: ActorRef): Props = Props(new Greeter(message, printerActor))
  final case class WhoToGreet(who: String)
  case object Greet
}

class Greeter(message: String, printerActor: ActorRef) extends Actor {
  import Greeter._
  import Printer._

  var greeting = ""

  def receive = {
    case WhoToGreet(who) =>
      greeting = message + ", " + who
    case Greet           =>
      printerActor ! Greeting(greeting)
  }
}

case class Hello(sender: String)
case class UpdatePosition(delta: Int, actorList: List[ActorRef])
case object PrintPosition
case class NearbyProbe(x: Int, y: Int)
case object CharacterInfo
case class CollisionProbe(x: Int, y: Int, w: Int, h: Int)

abstract class Entity() {
  val name: String
  var room: Room
  def onCollision(entity: Entity)
  def changeRoom(room: Room, newX: Float, newY: Float)
}

abstract class Door(val name: String, var room: Room) extends Entity{
  var x: Int = room.w/2 - 20
  var y: Int = room.h-40
  var w = 48.0f
  var h = 77.0f

  override def changeRoom(room: Room, newX: Float, newY: Float): Unit = {

  }
}

class Room(val name: String, val x: Int, val y: Int, val w: Int, val h: Int) {
  def addCharacter(character: Character) = {
    characterList += character
  }

  def removeCharacter(character: Character) = {
    characterList -= character
  }

  val characterList: ListBuffer[Character] = ListBuffer[Character]()
  val doorList: ListBuffer[Door] = ListBuffer[Door]()

  def init(): Unit = {

  }

  def render(g: Graphics, doorImage: Image, offsetX: Float, offsetY: Float): Unit = {
    g.setColor(Color.gray)
    g.fillRect(x - offsetX, y - offsetY, w, h)


    doorList.foreach(door => {
      g.drawImage(doorImage, x + door.x - offsetX, y + door.y - offsetY)
    })

    characterList.foreach(character => {
      g.setColor(Color.cyan)
      g.fillRect(x + character.x - offsetX, y + character.y - offsetY, character.w, character.h)
    })

    characterList.foreach(character => {
      g.setColor(Color.darkGray)
      g.drawString(character.name, x + character.x - 10 - offsetX, y + character.y - 25 - offsetY)
    })
  }

  def update(delta: Int): Unit = {
    characterList.foreach(character => {
      character.update(delta)
    })
  }
}


class Character(val name: String, var room: Room) extends Entity {
  var w: Float = Globals.CHARACTER_SIZE
  var h: Float = Globals.CHARACTER_SIZE
  var x: Float = Random.nextInt(room.w - w.toInt)
  var y: Float = Random.nextInt(room.h - h.toInt)

  var currentVelocityX: Float = 0.0f
  var currentVelocityY: Float = 0.0f
  var timer: Int = 0
  var speed: Float = 0.25f

  def update(delta: Int): Unit = {
    timer = timer + delta
    if(timer > 300) {
      val inPlace = if (Random.nextInt(100) < 30) true else false

      timer = 0
      if (inPlace) {
        currentVelocityX = 0
        currentVelocityY = 0
      }
      else {
        currentVelocityX = (Random.nextInt(3) - 1) * speed
        currentVelocityY = (Random.nextInt(3) - 1) * speed
      }

    }


    var collided = false

    room.characterList.filter(character => character != this).foreach(character => {
      if (checkCollision(character.x, character.y, character.w, character.h)) {
        collided = true
        onCollision(character)
        character.onCollision(this)
      }
    })

    room.doorList.foreach(door => {
      if (checkCollision(door.x, door.y, door.w, door.h)) {
        println("door collision! x: " + door.x + " y: " + door.y + " w: " + door.w + " h: " + door.h + " collided with x: " + x + " y: " + y + " w: " + w + " h: " + h )
        onCollision(door)
        door.onCollision(this)
      }
    })

    if (this.x + currentVelocityX < 0 || this.x + currentVelocityX > room.w - this.w) collided = true
    if (this.y + currentVelocityY < 0 || this.y + currentVelocityY > room.h - this.h) collided = true

    if (!collided) {
      this.x += currentVelocityX
      this.y += currentVelocityY
    }

  }

  def checkCollision(thatX: Float, thatY: Float, thatW: Float, thatH: Float): Boolean = {

    if (x + currentVelocityX < thatX + thatW &&
      x + currentVelocityX + w > thatX &&
      y + currentVelocityY < thatY + thatH &&
      h + y + currentVelocityY > thatY) true
    else false

  }

  override def onCollision(entity: Entity): Unit = {
    println("this character " + name + " collided with " + entity.name)
  }

  def changeRoom(newRoom: Room, newX: Float, newY: Float): Unit = {
    room.removeCharacter(this)
    newRoom.addCharacter(this)

    room = newRoom
    x = newX
    y = newY

  }
}

class CharacterActor(val name: String, val character: Character) extends Actor with ActorLogging {

  val char: Character = character


  override def receive: Receive = {
    case Hello(sender) => log.info(sender + " says hello to " + name)
    case UpdatePosition(delta, actorList) =>


    case NearbyProbe(thatX, thatY) if Math.abs(thatX - char.x) <= 25 && Math.abs(thatY - char.y) <= 25 && sender != self => {
      sender ! Hello(name)
    }
    case CharacterInfo => sender ! (name, char.x, char.y)

    case CollisionProbe(thatX, thatY, thatW, thatH) =>

  }


}


object Printer {
  def props: Props = Props[Printer]
  final case class Greeting(greeting: String)
}

class Printer extends Actor with ActorLogging {
  import Printer._

  def receive = {
    case Greeting(greeting) =>
      log.info("Greeting received (from " + sender() + "): " + greeting)
  }
}


class SimulationSlickGame(gameName: String) extends BasicGame(gameName) {
  val system: ActorSystem = ActorSystem("crowd_sim_system")

  // load resources
  var listOfNames = Array("Virgil", "Dominique", "Hermina",
    "Carolynn", "Adina", "Elida", "Classie", "Raymonde",
    "Lovie", "Theola", "Damion", "Petronila", "Corrinne",
    "Arica", "Alfonso", "Madalene", "Alvina", "Eliana", "Jarrod", "Thora")

  var doorImage: Image = _

  var viewX: Float = 0.0f
  var viewY: Float = 0.0f

  var roomList: ListBuffer[Room] = new ListBuffer[Room]

  var mutableActorList = new ListBuffer[ActorRef]()




  var actorList: List[ActorRef] = mutableActorList.toList

  var timer = 0

  override def init(gc: GameContainer): Unit = {
    doorImage = new Image("door.png")

    val room1 = new Room("default room", 200, 30, 1100, 700)

    val room2 = new Room("other room", 200, -800, 700, 500)


    for(_ <- 1 to 7)
    {
      val randomNameIndex = Random.nextInt(listOfNames.length)
      val randomName = listOfNames(randomNameIndex)
      listOfNames = listOfNames.take(randomNameIndex) ++ listOfNames.drop(randomNameIndex+1)
      val character = new Character(randomName, room1)
      room1.characterList += character
      val actor = system.actorOf(Props(new CharacterActor(randomName, character)))
      mutableActorList += actor
    }


    room1.doorList += new Door("some door", room1) {
      override def onCollision(entity: Entity): Unit = entity.changeRoom(room2, 50, 50)
    }

    roomList += room1
    roomList += room2
  }

  override def update(gc: GameContainer, i: Int): Unit = {
    //      actorList.foreach(actor => actor ! PrintPosition)

    if (gc.getInput.isKeyDown(Input.KEY_DOWN)) {
      viewY = viewY + (1.0f * i.toFloat)
    }
    if (gc.getInput.isKeyDown(Input.KEY_UP)) {
      viewY = viewY - (1.0f * i.toFloat)
    }
    if (gc.getInput.isKeyDown(Input.KEY_RIGHT)) {
      viewX = viewX + (1.0f * i.toFloat)
    }
    if (gc.getInput.isKeyDown(Input.KEY_LEFT)) {
      viewX = viewX - (1.0f * i.toFloat)
    }

    roomList.foreach(room => {
      room.update(i)
    })

    roomList.foreach(room => {
      println(room.name + "size : " + room.characterList.length)
    })
  }


  override def render(gc: GameContainer, g: Graphics): Unit = {

//    val mutableResponses = ListBuffer[(String, Float, Float)]()

    roomList.foreach(room => {
      room.render(g, doorImage, viewX, viewY)
    })
  }
}

object CrowdSim extends App {
  import org.newdawn.slick.AppGameContainer

  var gameContainer = new AppGameContainer(new SimulationSlickGame("Simple Slick Game"))
  gameContainer.setDisplayMode(1440, 900, false)
  gameContainer.start()
}