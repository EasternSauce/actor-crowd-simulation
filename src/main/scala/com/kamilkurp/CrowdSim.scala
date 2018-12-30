package com.kamilkurp

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.util.Random
import org.newdawn.slick._


import scala.collection.mutable.ListBuffer
import scala.language.postfixOps


object Globals {
  val LEVEL_X: Int = 200
  val LEVEL_Y: Int = 30
  val LEVEL_WIDTH: Int = 1100
  val LEVEL_HEIGHT: Int = 700
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

class Character(val name: String, val characterList: ListBuffer[Character]) {
  var x: Float = Random.nextInt(Globals.LEVEL_WIDTH - Globals.CHARACTER_SIZE)
  var y: Float = Random.nextInt(Globals.LEVEL_HEIGHT - Globals.CHARACTER_SIZE)
  var w: Float = Globals.CHARACTER_SIZE
  var h: Float = Globals.CHARACTER_SIZE

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

    characterList.filter(character => character != this).foreach(character => {
      if (checkCollision(character.x, character.y, character.w, character.h)) collided = true
    })

    if (!collided) move(currentVelocityX, currentVelocityY)

  }

  def move(x: Float, y: Float): Unit = {
     this.x += x
    if (this.y + y >= 0 && this.y + y < Globals.LEVEL_HEIGHT - Globals.CHARACTER_SIZE) this.y += y
  }

  def checkCollision(thatX: Float, thatY: Float, thatW: Float, thatH: Float): Boolean = {
    var collision = false

    if (this.x + currentVelocityX < 0 || this.x + currentVelocityX > Globals.LEVEL_WIDTH - Globals.CHARACTER_SIZE) collision = true
    if (this.y + currentVelocityY < 0 || this.y + currentVelocityY > Globals.LEVEL_WIDTH - Globals.CHARACTER_SIZE) collision = true


    if (x + currentVelocityX < thatX + thatW &&
      x + currentVelocityX + w > thatX &&
      y + currentVelocityY < thatY + thatH &&
      h + y + currentVelocityY > thatY) {
      // collision detected!
      println("collision detected! rect: " + x + " " + y + " " + w  + " " + h + "with rect: "+ thatX + " " + thatY + " " + thatW  + " " + thatH)
      collision = true
    }

    collision
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

  var listOfNames = Array("Virgil", "Dominique", "Hermina",
    "Carolynn", "Adina", "Elida", "Classie", "Raymonde",
    "Lovie", "Theola", "Damion", "Petronila", "Corrinne",
    "Arica", "Alfonso", "Madalene", "Alvina", "Eliana", "Jarrod", "Thora")

  import scala.collection.mutable.ListBuffer

  var mutableActorList = new ListBuffer[ActorRef]()
  var characterList: ListBuffer[Character] = ListBuffer[Character]()

  for(_ <- 1 to 7)
  {
    val randomNameIndex = Random.nextInt(listOfNames.length)
    val randomName = listOfNames(randomNameIndex)
    listOfNames = listOfNames.take(randomNameIndex) ++ listOfNames.drop(randomNameIndex+1)
    val character = new Character(randomName, characterList)
    characterList += character
    val actor = system.actorOf(Props(new CharacterActor(randomName, character)))
    mutableActorList += actor
  }

  var actorList: List[ActorRef] = mutableActorList.toList

  var timer = 0

  override def init(gc: GameContainer): Unit = {

  }

  override def update(gc: GameContainer, i: Int): Unit = {
//      actorList.foreach(actor => actor ! PrintPosition)

    characterList.foreach(character => {
      character.update(i)
    })
//      actorList.foreach(actor => {
//        actor ! UpdatePosition(i, actorList)
//      })
  }

  override def render(gc: GameContainer, g: Graphics): Unit = {
    import scala.concurrent.duration._

    g.setColor(Color.gray)
    g.fillRect(Globals.LEVEL_X, Globals.LEVEL_Y, Globals.LEVEL_WIDTH, Globals.LEVEL_HEIGHT)

//    val mutableResponses = ListBuffer[(String, Float, Float)]()


    characterList.foreach(character => {
      g.setColor(Color.cyan)
      g.fillRect(Globals.LEVEL_X + character.x, Globals.LEVEL_Y + character.y, character.w, character.h)
    })

    characterList.foreach(character => {
      g.setColor(Color.darkGray)
      g.drawString(character.name, Globals.LEVEL_X + character.x - 10, Globals.LEVEL_Y + character.y - 25)
    })


  }

}

object CrowdSim extends App {
  import org.newdawn.slick.AppGameContainer

  var gameContainer = new AppGameContainer(new SimulationSlickGame("Simple Slick Game"))
  gameContainer.setDisplayMode(1440, 900, false)
  gameContainer.start()
}