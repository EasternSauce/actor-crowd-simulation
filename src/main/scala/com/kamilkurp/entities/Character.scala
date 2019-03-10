package com.kamilkurp.entities

import akka.actor.ActorRef
import com.kamilkurp.ControlScheme.ControlScheme
import com.kamilkurp._
import com.kamilkurp.behaviors.{FollowingBehavior, RelaxedBehavior, RunToExitBehavior}
import org.newdawn.slick.geom.Vector2f
import org.newdawn.slick.{GameContainer, Image}

import scala.collection.mutable
import scala.util.Random

class Character(val name: String, var room: Room, val controlScheme: ControlScheme, var image: Image) extends Entity {

  override var w: Float = Globals.CHARACTER_SIZE
  override var h: Float = Globals.CHARACTER_SIZE
  override var x: Float = _
  override var y: Float = _
  override var currentVelocityX: Float = 0.0f
  override var currentVelocityY: Float = 0.0f

  var followingBehavior: FollowingBehavior = new FollowingBehavior(this)
  var runToExitBehavior: RunToExitBehavior = new RunToExitBehavior(this)
  var relaxedBehavior: RelaxedBehavior = new RelaxedBehavior(this)

  var viewAngle: Float = 0


  var behaviorSet: mutable.HashSet[String] = new mutable.HashSet[String]()
  behaviorSet += "relaxed"

  var controls: (Int, Int, Int, Int) = _

  var speed: Float = 0.5f

  var slow: Float = 0.0f
  var slowTimer: Int = 0

  var slowed: Boolean = false

  var actor: ActorRef = _

  var deviationX: Float = 0
  var deviationY: Float = 0

  var moveAway: Boolean = false
  var moveAwayX: Float = 0
  var moveAwayY: Float = 0
  var moveAwayTimer: Int = 0

  var isFree = false
  while (!isFree) {
    x = Random.nextInt(room.w - w.toInt)
    y = Random.nextInt(room.h - h.toInt)

    val collisionDetails = Globals.manageCollisions(room, this)
    if (!collisionDetails.colX || !collisionDetails.colY) {
      isFree = true
    }
  }

  if (Random.nextInt(100) < 15) behaviorSet += "runToExit"

  def this(name: String, room: Room, controlScheme: ControlScheme, controls: (Int, Int, Int, Int), image: Image) {
    this(name, room, controlScheme, image)
    this.controls = controls


  }

  def update(gc: GameContainer, delta: Int): Unit = {
    slowTimer = slowTimer + delta
    moveAwayTimer = moveAwayTimer + delta

    if (controlScheme == ControlScheme.Agent) updateAgent(delta)
    else if (controlScheme == ControlScheme.Manual) updateManual(gc)

    val collisionDetails = Globals.manageCollisions(room, this)
    if (!collisionDetails.colX) {
      this.x += currentVelocityX
    }
    if (!collisionDetails.colY) {
      this.y += currentVelocityY
    }
  }

  private def updateAgent(delta: Int): Unit = {
    if (slowTimer > 3000) {
      slow = 0f
    }

    if (moveAwayTimer > 500) {
      moveAway = false
    }

    if (behaviorSet.contains("following")) {
//      if (moveAway) {
//        if (timer > 500) {
//          deviationX = 0.3f * Random.nextFloat() - 0.15f
//          deviationY = 0.3f * Random.nextFloat() - 0.15f
//          timer = 0
//        }
//
//        val normalVector = new Vector2f(moveAwayX - this.x, moveAwayY - this.y)
//        normalVector.normalise()
//
//        println("before negating " + normalVector.x + " " + normalVector.y)
//        normalVector.negateLocal()
//        println("after negating " + normalVector.x + " " + normalVector.y)
//
//
//
//
//        currentVelocityX = (normalVector.x + deviationX) * speed * (1f - slow)
//        currentVelocityY = (normalVector.y + deviationY) * speed * (1f - slow)
//      }
//      else {
        followingBehavior.perform(delta)
//      }
    }
    else {
      if (behaviorSet.contains("runToExit")) {
        runToExitBehavior.perform(delta)
      }
      else if (behaviorSet.contains("relaxed")) {
        relaxedBehavior.perform(delta)
      }
    }
  }

  private def updateManual(gc: GameContainer): Unit = {
    var moved = false

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

    if (moved) {
      CameraView.x = room.x + x - Globals.WINDOW_X / 2 + w / 2
      CameraView.y = room.y + y - Globals.WINDOW_Y / 2 + h / 2
    }
  }

  def moveAwayFrom(x: Float, y: Float): Unit = {
    moveAway = true
    moveAwayX = x
    moveAwayY = y
    moveAwayTimer = 0
  }


  override def onCollision(entity: Entity): Unit = {
    //println("this character " + name + " collided with " + entity.name)

    if (entity.getClass == classOf[Character]) {
      slowTimer = 0
      slow = 0.2f
    }
  }

  def changeRoom(newRoom: Room, newX: Float, newY: Float): Unit = {
    room.removeCharacter(this)
    newRoom.addCharacter(this)

    room = newRoom
    x = newX
    y = newY
  }

  def setActor(actor: ActorRef): Unit = {
    this.actor = actor
  }
}
