package com.kamilkurp.agent

import akka.actor.ActorRef
import com.kamilkurp.building.{Door, Room}
import com.kamilkurp.entity.Entity
import com.kamilkurp.utils.ControlScheme.ControlScheme
import com.kamilkurp.utils.{ControlScheme, Globals, Timer}
import org.newdawn.slick.geom._
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable
import scala.util.Random

class Agent(val name: String, var room: Room, val controlScheme: ControlScheme, var image: Image) extends Entity with BehaviorManager {

  val rememberedRoute: mutable.Map[String, (Float, Float)] = mutable.Map[String, (Float, Float)]()
  val speed: Float = 0.5f
  val chanceToBeLeader: Float = 20
  override var currentVelocityX: Float = 0.0f
  override var currentVelocityY: Float = 0.0f
  override var shape: Shape = new Rectangle(0, 0, Globals.CHARACTER_SIZE, Globals.CHARACTER_SIZE)
  var walkAngle: Float = 0


  setBehavior("idle")
  var viewAngle: Float = 0
  var viewCone: ViewCone = new ViewCone(this)
  var controls: (Int, Int, Int, Int) = _
  var slow: Float = 0.0f
  var slowTimer: Timer = new Timer(3000)
  var lookTimer: Timer = new Timer(50)
  var slowed: Boolean = false
  var actor: ActorRef = _
  var deviationX: Float = 0
  var deviationY: Float = 0
  var atDoor: Boolean = false

  if (Random.nextInt(100) < chanceToBeLeader) {
    setBehavior("leader")
  }

  var followX: Float = 0
  var followY: Float = 0
  var followDistance: Float = 0

  var followedAgent: Agent = _

  var outOfWayTimer: Timer = new Timer(1000)
  outOfWayTimer.set(outOfWayTimer.timeout)

  var movingOutOfTheWay: Boolean = false

  var lastSeenFollowedEntityTimer = new Timer(1000 + new Random().nextInt(600))

  var lostSightOfFollowedEntity: Boolean = false

  var isFree = false

  var doorToEnter: Door = _

  while (!isFree) {
    shape.setX(Random.nextInt(room.w - Globals.CHARACTER_SIZE))
    shape.setY(Random.nextInt(room.h - Globals.CHARACTER_SIZE))

    val collisionDetails = Globals.manageCollisions(room, this)

    if (!collisionDetails.colX && !collisionDetails.colY) {
      isFree = true
    }

  }


  if (name == "Player") {
    setBehavior("leader")
  }

  def this(name: String, room: Room, controlScheme: ControlScheme, controls: (Int, Int, Int, Int), image: Image) {
    this(name, room, controlScheme, image)
    this.controls = controls
  }

  def update(gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    slowTimer.update(delta)
    lookTimer.update(delta)

    viewCone.update(delta)

    if (lookTimer.timedOut() && walkAngle != viewAngle) {
      adjustViewAngle(Agent.findSideToTurn(viewAngle, walkAngle))

      lookTimer.reset()
    }

    if (slowTimer.timedOut()) {
      slow = 0f
    }

    if (controlScheme == ControlScheme.Agent) {
      getBehavior(currentBehavior).perform(delta)
    }
    else if (controlScheme == ControlScheme.Manual) {
      ControlScheme.handleManualControls(this, gc, delta, renderScale)
    }

    val collisionDetails = Globals.manageCollisions(room, this)
    if (!collisionDetails.colX) {
      shape.setX(shape.getX + currentVelocityX)
    }
    if (!collisionDetails.colY) {
      shape.setY(shape.getY + currentVelocityY)
    }


  }


  private def adjustViewAngle(clockwise: Boolean): Unit = {
    val turnSpeed = 12
    if (Math.abs(viewAngle - walkAngle) > turnSpeed && Math.abs((viewAngle + 180) % 360 - (walkAngle + 180) % 360) > turnSpeed) {
      if (clockwise) { // clockwise
        if (viewAngle + turnSpeed < 360) viewAngle += turnSpeed
        else viewAngle = viewAngle + turnSpeed - 360
      }
      else { // counterclockwise
        if (viewAngle - turnSpeed > 0) viewAngle -= turnSpeed
        else viewAngle = viewAngle - turnSpeed + 360
      }
    }
    else {
      viewAngle = walkAngle
    }
  }

  override def onCollision(entity: Entity): Unit = {
    if (entity.getClass == classOf[Agent]) {
      slowTimer.reset()
      slow = 0.2f
    }
  }

  override def changeRoom(entryDoor: Door, newX: Float, newY: Float): Unit = {
    if (controlScheme != ControlScheme.Manual && doorToEnter != entryDoor) return

    atDoor = false


    getBehavior("follow").timer.start()

    val newRoom: Room = entryDoor.leadingToDoor.room

    if (rememberedRoute.contains(newRoom.name)) {
      followX = rememberedRoute(newRoom.name)._1
      followY = rememberedRoute(newRoom.name)._2
      followDistance = 0
    }
    else {
      followX = newRoom.w / 2
      followY = newRoom.h / 2
      followDistance = 0
    }

    for (character <- room.agentList) {
      if (character != this) {
        character.actor ! AgentEnteredDoor(this, entryDoor, entryDoor.shape.getX, entryDoor.shape.getY)
      }
    }

    room.removeCharacter(this)
    newRoom.addCharacter(this)

    room = newRoom
    shape.setX(newX)
    shape.setY(newY)
  }

  def setActor(actor: ActorRef): Unit = {
    this.actor = actor
  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.drawImage(image, room.x + shape.getX - offsetX, room.y + shape.getY - offsetY)

    viewCone.update(15) // workaround - otherwise cone not drawn properly
    viewCone.draw(g, offsetX, offsetY)
  }

  def drawName(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.setColor(Color.pink)
    g.drawString(name, room.x + shape.getX - 10 - offsetX, room.y + shape.getY - 40 - offsetY)
    if (currentBehavior == "idle") g.setColor(Color.cyan)
    if (currentBehavior == "follow" && !lostSightOfFollowedEntity) g.setColor(Color.yellow)
    if (currentBehavior == "follow" && lostSightOfFollowedEntity) g.setColor(Color.green)
    if (currentBehavior == "leader") g.setColor(Color.red)
    if (currentBehavior == "holdMeetPoint") g.setColor(Color.green)

    var tag: String = ""
    if (currentBehavior == "follow" && followedAgent != null) {
      tag = "[" + currentBehavior + " " + followedAgent.name + "]"
    }
    else {
      tag = "[" + currentBehavior + "]"

    }
    g.drawString(tag, room.x + shape.getX - 10 - offsetX, room.y + shape.getY - 25 - offsetY)

  }

}


object Agent {
  def findSideToTurn(currentAngle: Float, desiredAngle: Float): Boolean = {
    var clockwise = false
    if (currentAngle < 180) {
      if (desiredAngle - currentAngle >= 0 && desiredAngle - currentAngle < 180) clockwise = true
      else clockwise = false
    }
    else {
      if (currentAngle - desiredAngle >= 0 && currentAngle - desiredAngle < 180) clockwise = false
      else clockwise = true

    }
    clockwise
  }

}