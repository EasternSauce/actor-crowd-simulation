package com.kamilkurp

import org.newdawn.slick.GameContainer
import org.newdawn.slick.geom.Vector2f

import com.kamilkurp.entities.Character

object ControlScheme extends Enumeration {
  type ControlScheme = Value
  val Manual, Static, Agent = Value

  def handleManualControls(character: Character, gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    var moved = false

    if (gc.getInput.isKeyDown(character.controls._1)) {
      character.currentVelocityX = -character.speed * delta
      moved = true
    }
    else if (gc.getInput.isKeyDown(character.controls._2)) {
      character.currentVelocityX = character.speed * delta
      moved = true
    }
    else {
      character.currentVelocityX = 0
    }
    if (gc.getInput.isKeyDown(character.controls._3)) {
      character.currentVelocityY = -character.speed * delta
      moved = true
    }
    else if (gc.getInput.isKeyDown(character.controls._4)) {
      character.currentVelocityY = character.speed * delta
      moved = true
    }
    else {
      character.currentVelocityY = 0
    }

    if (character.currentVelocityX != 0 || character.currentVelocityY != 0) {
      val normalVector = new Vector2f(character.currentVelocityX, character.currentVelocityY)
      normalVector.normalise()

      character.walkAngle = normalVector.getTheta.floatValue()
    }

    if (moved) {
      CameraView.x = character.room.x + character.shape.getX - Globals.WINDOW_X/ renderScale/ 2 + character.shape.getWidth / 2
      CameraView.y = character.room.y + character.shape.getY - Globals.WINDOW_Y/renderScale / 2 + character.shape.getHeight / 2
    }
  }
}
