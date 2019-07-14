package com.kamilkurp.agent

import akka.actor.ActorRef
import com.kamilkurp.behavior._
import com.kamilkurp.building.{Door, MeetPoint, Room}
import com.kamilkurp.entity.Entity
import com.kamilkurp.util.ControlScheme.ControlScheme
import com.kamilkurp.util.{Configuration, ControlScheme, Globals, Timer}
import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge}
import org.newdawn.slick.geom.{Shape, _}
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.util.Random

class Agent private(var name: String, var currentRoom: Room, val controlScheme: ControlScheme, var image: Image, var buildingPlanGraph: DefaultDirectedWeightedGraph[Room, DefaultWeightedEdge]) extends Entity {


  override var shape: Shape = _
  override var debug: Boolean = _

  var controls: (Int, Int, Int, Int) = _

  var actor: ActorRef = _

  var startingPosX: Float = _
  var startingPosY: Float = _

  var intendedDoor: Door = _

  var visionModule: VisionModule = _
  var behaviorModule: BehaviorModule = _
  var movementModule: MovementModule = _
  var spatialModule: SpatialModule = _

  var avoidFireTimer: Timer = _
  var followTimer: Timer = _


  var followX: Float = _
  var followY: Float = _
  var followDistance: Float = _
  var followedAgent: Agent = _

  var startingRoom: Room = _

  var mousedOver: Boolean = _
  var selected: Boolean = _

  var personalSpeed: Float = _

  var stressLevel: Float = _

  var stressResistance: Float = _

  var unconscious: Boolean = _


  def setControls(controls: (Int, Int, Int, Int)): Unit = {
    this.controls = controls
  }

  def update(gc: GameContainer, delta: Int, renderScale: Float): Unit = {



    mousedOver = false

    if (!unconscious) {
      visionModule.update(delta)

      movementModule.update(gc, delta, renderScale)

      for (flames <- currentRoom.flamesList) {
        if (getDistanceTo(flames) < 70) {
          unconscious = true
        }
      }
    }

  }

  override def onCollision(entity: Entity): Unit = {
    if (entity.getClass == classOf[Agent]) {

      val that: Agent = entity.asInstanceOf[Agent]

      if (that.currentBehavior.name == FollowBehavior.name || that.currentBehavior.name == SearchExitBehavior.name) {
        movementModule.pushBack(this, that)
      }

    }

    //temporary solution, move evacuated outside map
    if (entity.getClass == classOf[MeetPoint]) {
      shape.setX(10000)
      shape.setY(10000)
      currentRoom.agentList -= this
    }

    if (entity.getClass == classOf[Door]) {
      val door = entity.asInstanceOf[Door]
      val leadingToDoor = door.leadingToDoor
      var normalVector = new Vector2f(movementModule.currentVelocityX, movementModule.currentVelocityY)

      normalVector.normalise()

      for (_ <- 1 to 36) {
        normalVector.setTheta(normalVector.getTheta + 10)
        val spotX = leadingToDoor.posX + normalVector.x * 50
        val spotY = leadingToDoor.posY + normalVector.y * 50

        if (!Globals.isRectOccupied(leadingToDoor.currentRoom, spotX - 5, spotY - 5, shape.getWidth + 10, shape.getHeight + 10, this)) {
          changeRoom(door, spotX, spotY)
          return
        }
      }
    }
  }

  override def changeRoom(entryDoor: Door, newX: Float, newY: Float): Unit = {
    if (intendedDoor != entryDoor) {
      if (controlScheme != ControlScheme.Manual) {
        return
      }
    }

    followTimer.reset()

    val newRoom: Room = entryDoor.leadingToDoor.currentRoom

    currentRoom.removeAgent(this)
    newRoom.addAgent(this)

    currentRoom = newRoom
    shape.setX(newX)
    shape.setY(newY)

    currentBehavior.onChangeRoom()
  }

  def currentBehavior: Behavior = behaviorModule.currentBehavior

  def setActor(actor: ActorRef): Unit = {
    this.actor = actor
  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    if (selected) {
      g.setColor(Color.green)
      g.drawRect(currentRoom.x + shape.getX - offsetX - 50, currentRoom.y + shape.getY - offsetY - 50, shape.getWidth + 100, shape.getHeight + 100)
    }
    else if (mousedOver) {
      g.setColor(Color.red)
      g.drawRect(currentRoom.x + shape.getX - offsetX - 50, currentRoom.y + shape.getY - offsetY - 50, shape.getWidth + 100, shape.getHeight + 100)
    }

    visionModule.draw(g, offsetX, offsetY)
    g.drawImage(image, currentRoom.x + shape.getX - offsetX, currentRoom.y + shape.getY - offsetY)
  }

  def drawName(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.setColor(Color.pink)
    if (movementModule.beingPushed) {
      g.setColor(Color.blue)
    }
    if (movementModule.isTripped) {
      g.setColor(Color.yellow)
    }
    g.drawString(name, currentRoom.x + shape.getX - 10 - offsetX, currentRoom.y + shape.getY - 40 - offsetY)
    g.setColor(currentBehavior.color)

    var tag: String = ""
    if (currentBehavior.name == FollowBehavior.name && followedAgent.name != null) {
      tag = "[" + currentBehavior.name + " " + followedAgent.name + "]"
    }
    else {
      tag = "[" + currentBehavior.name + "]"

    }
    g.drawString(tag, currentRoom.x + shape.getX - 10 - offsetX, currentRoom.y + shape.getY - 25 - offsetY)

    if (unconscious) {
      g.setColor(Color.red)
      g.drawString("[unconscious]", currentRoom.x + shape.getX - 10 - offsetX, currentRoom.y + shape.getY - 10 - offsetY)

    }

  }

  def followLeader(leader: Agent): Unit = {
    followedAgent = leader
    followX = leader.shape.getCenterX
    followY = leader.shape.getCenterY
    followDistance = 120
    changeBehavior(FollowBehavior.name)
  }

  def changeBehavior(behaviorName: String): Unit = {
    behaviorModule.setBehavior(behaviorName)
    behaviorModule.currentBehavior.init()
  }

  def broadcast(msg: AgentMessage, timer: Timer): Unit = {
    if (timer.timedOut()) {
      currentRoom.agentList.foreach(that => {
        if (getDistanceTo(that) < Configuration.agentBroadcastDistance && that != this) {
          that.actor ! msg
        }
      })
      timer.reset()
    }
  }
}

object Agent {
  def apply(name: String, room: Room, controlScheme: ControlScheme, image: Image, buildingPlanGraph: DefaultDirectedWeightedGraph[Room, DefaultWeightedEdge]): Agent = {
    val agent = new Agent(name, room, controlScheme, image, buildingPlanGraph)

    agent.shape = new Rectangle(0, 0, Globals.AGENT_SIZE, Globals.AGENT_SIZE)

    agent.followTimer = new Timer(Configuration.AGENT_FOLLOW_TIMER)


    agent.debug = false

    agent.avoidFireTimer = new Timer(500)
    agent.avoidFireTimer.time = agent.avoidFireTimer.timeout + 1

    agent.visionModule = VisionModule(agent)
    agent.movementModule = MovementModule(agent)
    agent.spatialModule = SpatialModule(agent)
    agent.behaviorModule = BehaviorModule(agent)


    agent.behaviorModule.setBehavior(agent.behaviorModule.startBehavior)


    var isFree = false

    while (!isFree) {
      agent.shape.setX(Random.nextInt(room.w - Globals.AGENT_SIZE))
      agent.shape.setY(Random.nextInt(room.h - Globals.AGENT_SIZE))

      val collisionDetails = Globals.manageCollisions(room, agent, 0, 0)

      if (!collisionDetails.colX && !collisionDetails.colY) {
        isFree = true
      }
    }

    agent.startingPosX = agent.shape.getX
    agent.startingPosY = agent.shape.getY

    agent.followX = 0
    agent.followY = 0
    agent.followDistance = 0

    agent.followedAgent = null

    agent.followTimer = new Timer(Configuration.AGENT_FOLLOW_TIMER)

    agent.startingRoom = agent.currentRoom

    agent.mousedOver = false
    agent.selected = false

    agent.personalSpeed = Configuration.AGENT_SPEED - 0.02f + Random.nextInt(5) * 0.01f

    agent.stressLevel = 0f

    agent.stressResistance = Random.nextFloat()


    agent.unconscious = false


    agent.spatialModule.setupMentalMap()


    agent.behaviorModule.currentBehavior.init()


    agent
  }
}