package com.kamilkurp.entities

import akka.actor.ActorRef
import com.kamilkurp.ControlScheme.ControlScheme
import com.kamilkurp._
import com.kamilkurp.behaviors._
import com.kamilkurp.utils.Timer
import org.newdawn.slick.geom._
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class Character(val name: String, var room: Room, val controlScheme: ControlScheme, var image: Image) extends Entity {

  override var currentVelocityX: Float = 0.0f
  override var currentVelocityY: Float = 0.0f
  override var shape: Shape = new Rectangle(0, 0, Globals.CHARACTER_SIZE, Globals.CHARACTER_SIZE)
//  override var allowChangeRoom: Boolean = false

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
//    println("looking for free spot for " + name)
    shape.setX(Random.nextInt(room.w - Globals.CHARACTER_SIZE))
    shape.setY(Random.nextInt(room.h - Globals.CHARACTER_SIZE))

    val collisionDetails = Globals.manageCollisions(room, this)

    if (!collisionDetails.colX && !collisionDetails.colY) {
      isFree = true
//      println("found spot at " + shape.getX + " " + shape.getY)
    }

  }


  if (name == "Player") {
//    println("setting for player")
    setBehavior("leader")
  }

  def this(name: String, room: Room, controlScheme: ControlScheme, controls: (Int, Int, Int, Int), image: Image) {
    this(name, room, controlScheme, image)
    this.controls = controls



  }

  def update(gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    slowTimer.update(delta)
    lookTimer.update(delta)

    viewCone.update()

//    if (currentVelocityX == 0 && currentVelocityY == 0) {
//      println(name + "\'s followX: " + followX + " followY: " + followY)
//    }

    if (controlScheme == ControlScheme.Agent) updateAgent(delta)
    else if (controlScheme == ControlScheme.Manual) updateManual(gc, delta, renderScale)

    val collisionDetails = Globals.manageCollisions(room, this)
    if (!collisionDetails.colX) {
      shape.setX(shape.getX + currentVelocityX)
    }
    if (!collisionDetails.colY) {
      shape.setY(shape.getY + currentVelocityY)
    }


  }

  private def updateAgent(delta: Int): Unit = {
    if (slowTimer.timedOut()) {
      slow = 0f
    }

    if (lookTimer.timedOut() && walkAngle != viewAngle) {
      adjustViewAngle(Character.findSideToTurn(viewAngle, walkAngle))

      lookTimer.reset()
    }

    getBehavior(currentBehavior).perform(this, delta)
  }

  private def updateManual(gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    var moved = false

    if (lookTimer.timedOut() && walkAngle != viewAngle) {
      adjustViewAngle(Character.findSideToTurn(viewAngle, walkAngle))
      lookTimer.reset()
    }

    if (gc.getInput.isKeyDown(controls._1)) {
      currentVelocityX = -speed
      moved = true
    }
    else if (gc.getInput.isKeyDown(controls._2)) {
      currentVelocityX = speed
      moved = true
    }
    else {
      currentVelocityX = 0
    }
    if (gc.getInput.isKeyDown(controls._3)) {
      currentVelocityY = -speed
      moved = true
    }
    else if (gc.getInput.isKeyDown(controls._4)) {
      currentVelocityY = speed
      moved = true
    }
    else {
      currentVelocityY = 0
    }

    if (currentVelocityX != 0 || currentVelocityY != 0) {
      val normalVector = new Vector2f(currentVelocityX, currentVelocityY)
      normalVector.normalise()

      walkAngle = normalVector.getTheta.floatValue()
    }

    if (moved) {
      CameraView.x = room.x + shape.getX - Globals.WINDOW_X/ renderScale/ 2 + shape.getWidth / 2
      CameraView.y = room.y + shape.getY - Globals.WINDOW_Y/renderScale / 2 + shape.getHeight / 2
    }

    getBehavior(currentBehavior).perform(this, delta)
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
    if (doorToEnter != entryDoor) return

    atDoor = false
//    lostSightOfFollowedEntity = true
//    if (!allowChangeRoom) return
//
//    if (currentBehavior != "leader") {
//      allowChangeRoom = false
//    }

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
//      println(name + ": setting follow to center of room")
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

//    if (allowChangeRoom) {
//      g.drawString("wants to enter door", room.x + shape.getX - 10 - offsetX, room.y + shape.getY + 25 - offsetY)
//    }

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

//    if (this.getDistanceTo(posX, posY) < 40+atDistance) {
//      println(name + " arrived")
//    }

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