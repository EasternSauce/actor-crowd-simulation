package com.kamilkurp.entities

import akka.actor.ActorRef
import com.kamilkurp.ControlScheme.ControlScheme
import com.kamilkurp._
import com.kamilkurp.behaviors._
import com.kamilkurp.utils.Timer
import org.newdawn.slick.geom._
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable
import scala.util.Random

class Character(val name: String, var room: Room, val controlScheme: ControlScheme, var image: Image) extends Entity {

  override var currentVelocityX: Float = 0.0f
  override var currentVelocityY: Float = 0.0f
  override var shape: Shape = new Rectangle(0, 0, Globals.CHARACTER_SIZE, Globals.CHARACTER_SIZE)

  var currentBehavior: String = _

  var walkAngle: Float = 0
  var viewAngle: Float = 0

  var viewCone: ViewCone = new ViewCone(this)

  val rememberedRoute: mutable.Map[String, (Float, Float)] = mutable.Map[String, (Float, Float)]()

  val behaviorMap: mutable.HashMap[String, Behavior] = mutable.HashMap.empty[String,Behavior]

  behaviorMap += ("follow" -> new FollowBehavior)
  behaviorMap += ("idle" -> new IdleBehavior)
  behaviorMap += ("leader" -> new LeaderBehavior)
  behaviorMap += ("holdMeetPoint" -> new HoldMeetPointBehavior)

  setBehavior("idle")

  var controls: (Int, Int, Int, Int) = _

  val speed: Float = 0.5f

  var slow: Float = 0.0f
  var slowTimer: Timer = new Timer(3000)

  var lookTimer: Timer = new Timer(50)

  var slowed: Boolean = false

  var actor: ActorRef = _

  var deviationX: Float = 0
  var deviationY: Float = 0

  val chanceToBeLeader: Float = 20

  var atDoor: Boolean = false

  if (Random.nextInt(100) < chanceToBeLeader) {
    setBehavior("leader")
  }

  var followX: Float = 0
  var followY: Float = 0
  var followDistance: Float = 0

  var followedCharacter: Character = _

  var outOfWayTimer: Timer = new Timer(1000)
  outOfWayTimer.set(outOfWayTimer.timeout)

  var movingOutOfTheWay: Boolean = false

  var lastSeenFollowedEntityTimer = new Timer(1000 + new Random().nextInt(600))

  var lostSightOfFollowedEntity: Boolean =  false

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
      adjustViewAngle(Character.findSideToTurn(viewAngle, walkAngle))

      lookTimer.reset()
    }

    if (slowTimer.timedOut()) {
      slow = 0f
    }

    if (controlScheme == ControlScheme.Agent) {
      getBehavior(currentBehavior).perform(this, delta)
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
    if (entity.getClass == classOf[Character]) {
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
      followX = newRoom.w/2
      followY = newRoom.h/2
      followDistance = 0
    }

    for (character <- room.characterList) {
      if (character != this) {
        character.actor ! CharacterEnteredDoor(this, entryDoor, entryDoor.shape.getX, entryDoor.shape.getY)
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
    if (currentBehavior == "follow") {
      tag = "[" + currentBehavior + " " + followedCharacter.name +  "]"
    }
    else {
      tag = "[" + currentBehavior + "]"

    }
    g.drawString(tag, room.x + shape.getX - 10 - offsetX, room.y + shape.getY - 25 - offsetY)

  }


  def getDistanceTo(entity: Entity): Float = {
    val differenceSquaredX = Math.pow(entity.shape.getCenterX.doubleValue() - shape.getCenterX.doubleValue(),2)
    val differenceSquaredY = Math.pow(entity.shape.getCenterY.doubleValue() - shape.getCenterY.doubleValue(),2)
    Math.sqrt(differenceSquaredX + differenceSquaredY).floatValue()
  }

  def getDistanceTo(x: Float, y: Float): Float = {
    val differenceSquaredX = Math.pow(x.doubleValue() - shape.getCenterX.doubleValue(),2)
    val differenceSquaredY = Math.pow(y.doubleValue() - shape.getCenterY.doubleValue(),2)
    Math.sqrt(differenceSquaredX + differenceSquaredY).floatValue()
  }






  def getBehavior(behaviorName: String): Behavior = behaviorMap(behaviorName)

  def setBehavior(behaviorName: String): Unit = {
    currentBehavior = behaviorName
    behaviorMap(behaviorName).init(this)
  }


  def follow(character: Character, posX: Float, posY: Float, atDistance: Float): Unit = {

    if (currentBehavior == "idle") {
      setBehavior("follow")
      followX = posX
      followY = posY
      followDistance = atDistance
      followedCharacter = character
      getBehavior("follow").timer.reset()
    }
    else if (currentBehavior == "follow") {
      if (character == followedCharacter) {
        followX = posX
        followY = posY
        getBehavior("follow").timer.reset()
        followDistance = atDistance
      }
      else {
        followX = posX
        followY = posY
        followDistance = atDistance
        followedCharacter = character
        getBehavior("follow").timer.reset()
      }
    }


  }

}


object Character {
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