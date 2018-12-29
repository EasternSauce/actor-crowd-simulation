package com.kamilkurp

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.util.Random
import org.newdawn.slick._

import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout

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
case class Move(delta: Int, actorList: List[ActorRef])
case object PrintPosition
case class NearbyProbe(x: Int, y: Int)
case object CharacterInfo

class Character(val name: String) extends Actor with ActorLogging {
  var x: Double = Random.nextInt(Globals.LEVEL_WIDTH - Globals.CHARACTER_SIZE)
  var y: Double = Random.nextInt(Globals.LEVEL_HEIGHT - Globals.CHARACTER_SIZE)
  var currentVelocityX: Double = 0.0
  var currentVelocityY: Double = 0.0
  var timer: Int = 0
  var speed: Double = 0.25
  override def receive: Receive = {
    case Hello(sender) => log.info(sender + " says hello to " + name)
    case Move(delta, actorList) =>
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

      move(currentVelocityX, currentVelocityY)
      actorList.foreach(actor => actor ! NearbyProbe(x.intValue(), y.intValue()))
    case NearbyProbe(thatX, thatY) if Math.abs(thatX - x) <= 25 && Math.abs(thatY - y) <= 25 && sender != self => {
      sender ! Hello(name)
    }
    case CharacterInfo => sender ! (name, x, y)
  }

  def move(x: Double, y: Double): Unit = {
    if (this.x + x >= 0 && this.x + x < Globals.LEVEL_WIDTH - Globals.CHARACTER_SIZE) this.x += x
    if (this.y + y >= 0 && this.y + y < Globals.LEVEL_HEIGHT - Globals.CHARACTER_SIZE) this.y += y
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

  for(_ <- 1 to 10)
  {
    val randomNameIndex = Random.nextInt(listOfNames.length)
    val randomName = listOfNames(randomNameIndex)
    listOfNames = listOfNames.take(randomNameIndex) ++ listOfNames.drop(randomNameIndex+1)
    val actor = system.actorOf(Props(new Character(randomName)))
    mutableActorList += actor
  }

  var actorList: List[ActorRef] = mutableActorList.toList

  var timer = 0

  override def init(gc: GameContainer): Unit = {

  }

  override def update(gc: GameContainer, i: Int): Unit = {
      actorList.foreach(actor => actor ! PrintPosition)

      actorList.foreach(actor => {
        actor ! Move(i, actorList)
      })
  }

  override def render(gc: GameContainer, g: Graphics): Unit = {
    import scala.concurrent.duration._

    g.setColor(Color.gray)
    g.fillRect(Globals.LEVEL_X, Globals.LEVEL_Y, Globals.LEVEL_WIDTH, Globals.LEVEL_HEIGHT)

    g.setColor(Color.orange)


    val mutableResponses = ListBuffer[(String, Double, Double)]()

    actorList.foreach(actor => {
      implicit val timeout: Timeout = Timeout(5 seconds)
      val future = actor ? CharacterInfo
      val result = Await.result(future, timeout.duration).asInstanceOf[(String, Double, Double)]

      mutableResponses += result
    })

    val responses = mutableResponses.toList

    responses.foreach(response => {
      val x = Globals.LEVEL_X + response._2.intValue()
      val y = Globals.LEVEL_Y + response._3.intValue()
      val w = Globals.CHARACTER_SIZE
      val h = Globals.CHARACTER_SIZE
      g.setColor(Color.cyan)
      g.fillRect(x, y, w, h)
    })

    responses.foreach(response => {
      val x = Globals.LEVEL_X + response._2.intValue()
      val y = Globals.LEVEL_Y + response._3.intValue()

      g.setColor(Color.darkGray)
      g.drawString(response._1, x - 10, y - 25)
    })


  }

}

object CrowdSim extends App {

  import org.newdawn.slick.AppGameContainer

  var gameContainer = new AppGameContainer(new SimulationSlickGame("Simple Slick Game"))
  gameContainer.setDisplayMode(1440, 900, false)
  gameContainer.start()
}