package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.util.Random
import org.newdawn.slick._

import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout

import scala.language.postfixOps


object Globals {
  val LEVEL_X = 200
  val LEVEL_Y = 30
  val LEVEL_WIDTH = 32
  val LEVEL_HEIGHT = 32
  val TILE_SIZE = 10
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
case class Move(x: Int, y: Int, actorList: List[ActorRef])
case object PrintPosition
case class NearbyProbe(x: Int, y: Int)
case object Coordinates

class Character(val name: String) extends Actor with ActorLogging {
  var x = 16
  var y = 16
  override def receive: Receive = {
    case Hello(sender) => log.info(sender + " says hello to " + name)
    case Move(x1, y1, actorList) => {
      move(x1, y1)
      actorList.foreach(actor => actor ! NearbyProbe(x, y))
    }
    case NearbyProbe(thatX, thatY) if Math.abs(thatX - x) <= 2 && Math.abs(thatY - y) <= 2 && sender != self => {
      sender ! Hello(name)
    }
    case Coordinates => sender ! (x, y)
  }

  def move(x: Int, y: Int): Unit = {
    if (this.x + x >= 0 && this.x + x < Globals.LEVEL_WIDTH) this.x += x
    if (this.y + y >= 0 && this.y + y < Globals.LEVEL_HEIGHT) this.y += y
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

object CrowdSim extends App {


  class SimulationSlickGame(gameName: String) extends BasicGame(gameName) {
    val system: ActorSystem = ActorSystem("crowd_sim_system")

    val actor1: ActorRef = system.actorOf(Props(new Character("bob")))

    val actor2: ActorRef = system.actorOf(Props(new Character("john")))

    val actorList = List(actor1, actor2)

    var timer = 0

    override def init(gc: GameContainer): Unit = {

    }

    override def update(gc: GameContainer, i: Int): Unit = {
      timer += i

      if (timer > 1000) {
        timer = 0
        actorList.foreach(actor => actor ! PrintPosition)

        actorList.foreach(actor => {
          val randX = Random.nextInt(3) - 1
          val randY = Random.nextInt(3) - 1
          actor ! Move(randX, randY, actorList)
        })
      }

    }

    override def render(gc: GameContainer, g: Graphics): Unit = {
      import scala.concurrent.duration._

      g.setColor(Color.gray)
      g.fillRect(Globals.LEVEL_X, Globals.LEVEL_Y, Globals.LEVEL_WIDTH * Globals.TILE_SIZE, Globals.LEVEL_HEIGHT * Globals.TILE_SIZE)

      g.setColor(Color.orange)
      actorList.foreach(actor => {
        implicit val timeout: Timeout = Timeout(5 seconds)
        val future = actor ? Coordinates
        val result = Await.result(future, timeout.duration).asInstanceOf[(Int, Int)]
        g.fillRect(Globals.LEVEL_X + result._1 * Globals.TILE_SIZE, Globals.LEVEL_Y + result._2 * Globals.TILE_SIZE, Globals.TILE_SIZE, Globals.TILE_SIZE)
      })

    }
  }

  object SimulationSlickGame extends App {

    import org.newdawn.slick.AppGameContainer

    var gameContainer = new AppGameContainer(new SimulationSlickGame("Simple Slick Game"))
    gameContainer.setDisplayMode(1024, 720, false)
    gameContainer.start()
  }

}
