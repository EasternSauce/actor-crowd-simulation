package com.kamilkurp

import akka.actor.ActorRef
import com.kamilkurp.ControlScheme.ControlScheme
import org.newdawn.slick.{GameContainer, Image}
import org.newdawn.slick.geom.Vector2f

import scala.util.Random

class Character(val name: String, var room: Room, val controlScheme: ControlScheme, var image: Image) extends Entity {
  override var w: Float = Globals.CHARACTER_SIZE
  override var h: Float = Globals.CHARACTER_SIZE
  override var x: Float = _
  override var y: Float = _
  override var currentVelocityX: Float = 0.0f
  override var currentVelocityY: Float = 0.0f

  var knowsWayOut: Boolean = _

  var controls: (Int, Int, Int, Int) = _

  var timer: Int = 0
  var speed: Float = 0.5f

  var slow: Float = 0.0f
  var slowTimer: Int = 0

  var slowed: Boolean = false

  var actor: ActorRef = _

  var deviationX: Float = 0
  var deviationY: Float = 0

  var isFree = false
  while (!isFree) {
    x = Random.nextInt(room.w - w.toInt)
    y = Random.nextInt(room.h - h.toInt)

    val collisionDetails = Globals.manageCollisions(room, this)
    if (!collisionDetails.colX || !collisionDetails.colY) {
      isFree = true
    }
  }

  knowsWayOut = Random.nextInt(100) < 60

  def this(name: String, room: Room, controlScheme: ControlScheme, controls: (Int, Int, Int, Int), image: Image) {
    this(name, room, controlScheme, image)
    this.controls = controls


  }

  def update(gc: GameContainer, delta: Int): Unit = {
    timer = timer + delta
    slowTimer = slowTimer + delta

    if (controlScheme == ControlScheme.Agent) updateAgent
    else if (controlScheme == ControlScheme.Manual) updateManual(gc)

    val collisionDetails = Globals.manageCollisions(room, this)
    if (!collisionDetails.colX) {
      this.x += currentVelocityX
    }
    if (!collisionDetails.colY) {
      this.y += currentVelocityY
    }
  }

  private def updateAgent: Unit = {
    val door = this.room.evacuationDoor

    if (slowTimer > 3000) {
      slow = 0f
    }

    if (knowsWayOut && door != null) {
      if (door != null) {
        if (timer > 500) {
          deviationX = 0.3f * Random.nextFloat() - 0.15f
          deviationY = 0.3f * Random.nextFloat() - 0.15f
          timer = 0

          val normalVector = new Vector2f(door.x - this.x, door.y - this.y)
          normalVector.normalise()

          currentVelocityX = (normalVector.x + deviationX) * speed * (1f - slow)
          currentVelocityY = (normalVector.y + deviationY) * speed * (1f - slow)
        }
      }
    }
    else {
      if (timer > 500) {
        val inPlace = Random.nextInt(100) < 60

        timer = 0
        if (inPlace) {
          currentVelocityX = 0
          currentVelocityY = 0
        }
        else {
          currentVelocityX = (Random.nextInt(3) - 1) * speed * (1f - slow) * 0.8f
          currentVelocityY = (Random.nextInt(3) - 1) * speed * (1f - slow) * 0.8f
        }

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
    timer = 500
  }

  def setActor(actor: ActorRef): Unit = {
    this.actor = actor
  }
}
