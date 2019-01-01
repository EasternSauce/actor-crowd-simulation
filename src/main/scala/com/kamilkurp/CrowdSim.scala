package com.kamilkurp

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.kamilkurp.ControlScheme.ControlScheme

import scala.util.Random
import org.newdawn.slick._

import scala.language.postfixOps
import scala.collection.mutable.ListBuffer


object Globals {
  val CHARACTER_SIZE: Int = 40

  private def intersects(entity: Entity, thatX: Float, thatY: Float, thatW: Float, thatH: Float): Boolean = {

    if (entity.x + entity.currentVelocityX < thatX + thatW &&
      entity.x + entity.currentVelocityX + entity.w > thatX &&
      entity.y + entity.currentVelocityY < thatY + thatH &&
      entity.h + entity.y + entity.currentVelocityY > thatY) true
    else false

  }

  def isColliding(room: Room, entity: Entity): Boolean = {
    var collided = false

    if (entity.x + entity.currentVelocityX < 0 || entity.x + entity.currentVelocityX > room.w - entity.w) collided = true
    if (entity.y + entity.currentVelocityY < 0 || entity.y + entity.currentVelocityY > room.h - entity.h) collided = true

    room.characterList.filter(character => character != entity).foreach(character => {
      if (intersects(entity, character.x, character.y, character.w, character.h)) {
        collided = true
        entity.onCollision(character)
        character.onCollision(entity)
      }
    })

    room.doorList.foreach(door => {
      if (intersects(entity, door.x, door.y, door.w, door.h)) {
        //println("door collision! x: " + door.x + " y: " + door.y + " w: " + door.w + " h: " + door.h + " collided with x: " + entity.x + " y: " + entity.y + " w: " + entity.w + " h: " + entity.h )
        entity.onCollision(door)
        door.onCollision(entity)
      }
    })


    collided
  }

  def isRectOccupied(room: Room, x: Float, y: Float, w: Float, h: Float): Boolean = {
    var occupied = false

    if (x < 0 || x > room.w - w) occupied = true
    if (y < 0 || y > room.h - h) occupied = true

    room.characterList.foreach(character => {
      if (intersects(character, x, y, w, h)) {
        occupied = true
      }
    })

    room.doorList.foreach(door => {
      if (intersects(door, x, y, w, h)) {
        occupied = true
      }
    })

    occupied
  }

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

  def receive: PartialFunction[Any, Unit] = {
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
  var x: Float
  var y: Float
  var w: Float
  var h: Float
  var currentVelocityX: Float
  var currentVelocityY: Float
  val name: String
  var room: Room
  def onCollision(entity: Entity)
  def changeRoom(room: Room, newX: Float, newY: Float)
}

class Door(val name: String, var room: Room) extends Entity{
  override var x: Float = room.w/2 - 20
  override var y: Float = room.h-40
  override var w = 48.0f
  override var h = 77.0f
  override var currentVelocityX = 0.0f
  override var currentVelocityY = 0.0f
  var leadingToDoor: Door = _

  override def changeRoom(room: Room, newX: Float, newY: Float): Unit = {

  }

  def connectWith(door: Door): Unit = {
    leadingToDoor = door
    door.leadingToDoor = this
  }

  override def onCollision(entity: Entity): Unit = {
    var foundSpot: (Float, Float) = null
    for (gridX <- Seq(-10-entity.w,(w-entity.w)/w,w+10)){
      for (gridY <- Seq(-10-entity.h,(h-entity.h)/h,h+10)){
        val potentialSpotX = leadingToDoor.x + gridX
        val potentialSpotY = leadingToDoor.y + gridY
        if (!Globals.isRectOccupied(leadingToDoor.room, potentialSpotX, potentialSpotY, entity.w, entity.h)) {
          foundSpot = (potentialSpotX, potentialSpotY)
        }
      }
    }
    if( foundSpot != null) {
      println("found a spot on " + foundSpot._1 + " " + foundSpot._2)
      println("leading door x" + leadingToDoor.x + " y " + leadingToDoor.y)

      entity.changeRoom(leadingToDoor.room, foundSpot._1, foundSpot._2)
      println("x: "+ entity.x + " y: " + entity.y)
    }
  }


}

class Room(val name: String, val x: Int, val y: Int, val w: Int, val h: Int) {
  def addCharacter(character: Character): characterList.type = {
    characterList += character
  }

  def removeCharacter(character: Character): characterList.type = {
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

  def update(gc: GameContainer, delta: Int): Unit = {
    characterList.foreach(character => {
      character.update(gc, delta)
    })
  }
}

object ControlScheme extends Enumeration {
  type ControlScheme = Value
  val Manual, Static, Random = Value
}

class Character(val name: String, var room: Room, val controlScheme: ControlScheme) extends Entity {
  override var w: Float = Globals.CHARACTER_SIZE
  override var h: Float = Globals.CHARACTER_SIZE
  override var x: Float = Random.nextInt(room.w - w.toInt)
  override var y: Float = Random.nextInt(room.h - h.toInt)

  override var currentVelocityX: Float = 0.0f
  override var currentVelocityY: Float = 0.0f

  var controls: (Int, Int, Int, Int) = _

  var timer: Int = 0
  var speed: Float = 0.25f

  def this(name: String, room: Room, controlScheme: ControlScheme, controls: (Int, Int, Int, Int)) {
    this(name, room, controlScheme)
    this.controls = controls
  }

  def update(gc: GameContainer, delta: Int): Unit = {
    timer = timer + delta

    if (controlScheme == ControlScheme.Random) {
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

      if (!Globals.isColliding(room, this)) {
        this.x += currentVelocityX
        this.y += currentVelocityY
      }
    }
    else if (controlScheme == ControlScheme.Manual) {
      val offset = speed * delta
      val oldX = x
      val oldY = y
      if (gc.getInput.isKeyDown(controls._1)) {
        x += -offset
      }
      if (gc.getInput.isKeyDown(controls._2)) {
        x += offset
      }
      if (gc.getInput.isKeyDown(controls._3)) {
        y += -offset
      }
      if (gc.getInput.isKeyDown(controls._4)) {
        y += offset
      }

      if (Globals.isColliding(room, this)) {
        println("reverting from " + x + " " + y + " to " + oldX + " " + oldY)

        x = oldX
        y = oldY

      }
      println("aaa: " + x + " " + y)


    }

  }



  override def onCollision(entity: Entity): Unit = {
    //println("this character " + name + " collided with " + entity.name)
  }

  def changeRoom(newRoom: Room, newX: Float, newY: Float): Unit = {
    room.removeCharacter(this)
    newRoom.addCharacter(this)

    println("changed room to " + newRoom.name)
    room = newRoom
    x = newX
    y = newY

    println("new coords: " + x + " " + y)

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


    for(_ <- 1 to 1)
    {
      val randomNameIndex = Random.nextInt(listOfNames.length)
      val randomName = listOfNames(randomNameIndex)
      listOfNames = listOfNames.take(randomNameIndex) ++ listOfNames.drop(randomNameIndex+1)
      val character = new Character(randomName, room1, ControlScheme.Random)
      room1.characterList += character
      val actor = system.actorOf(Props(new CharacterActor(randomName, character)))
      mutableActorList += actor
    }

    val playerName = "Player"
    val character = new Character(playerName, room1, ControlScheme.Manual, (Input.KEY_A, Input.KEY_D, Input.KEY_W, Input.KEY_S))
    room1.characterList += character

    val door1 = new Door("some door", room1)
    val door2 = new Door("some other door", room2)

    room1.doorList += door1
    room2.doorList += door2

    door1.connectWith(door2)

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
      room.update(gc, i)
    })

    //roomList.foreach(room => {
      //println(room.name + "size : " + room.characterList.length)
    //})
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