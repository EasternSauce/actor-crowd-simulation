package com.kamilkurp

import com.kamilkurp.ControlScheme.ControlScheme
import org.newdawn.slick.GameContainer

import scala.util.Random

class Character(val name: String, var room: Room, val controlScheme: ControlScheme) extends Entity {
  override var w: Float = Globals.CHARACTER_SIZE
  override var h: Float = Globals.CHARACTER_SIZE
  override var x: Float = _
  override var y: Float = _

  override var currentVelocityX: Float = 0.0f
  override var currentVelocityY: Float = 0.0f

  var controls: (Int, Int, Int, Int) = _

  var timer: Int = 0
  var speed: Float = 0.25f


  var isFree = false
  while (!isFree) {
    x = Random.nextInt(room.w - w.toInt)
    y = Random.nextInt(room.h - h.toInt)

    if (!Globals.isColliding(room, this)) {
      isFree = true
    }
  }


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

      var moved = false

      if (gc.getInput.isKeyDown(controls._1)) {
        x += -offset
        moved = true
      }
      if (gc.getInput.isKeyDown(controls._2)) {
        x += offset
        moved = true
      }
      if (gc.getInput.isKeyDown(controls._3)) {
        y += -offset
        moved = true
      }
      if (gc.getInput.isKeyDown(controls._4)) {
        y += offset
        moved = true
      }

      if (Globals.isColliding(room, this)) {
        println("reverting from " + x + " " + y + " to " + oldX + " " + oldY)

        x = oldX
        y = oldY

      }

      if (moved) {
        CameraView.x = room.x + x - Globals.WINDOW_X/2 + w/2
        CameraView.y = room.y + y - Globals.WINDOW_Y/2 + h/2
      }
    }
  }



  override def onCollision(entity: Entity): Unit = {
    //println("this character " + name + " collided with " + entity.name)
  }

  def changeRoom(newRoom: Room, newX: Float, newY: Float): Unit = {
    room.removeCharacter(this)
    newRoom.addCharacter(this)

    room = newRoom
    x = newX
    y = newY
  }
}
