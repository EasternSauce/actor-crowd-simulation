package com.kamilkurp

import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable.ListBuffer

class Room(val name: String, val x: Int, val y: Int, val w: Int, val h: Int) {
  def addCharacter(character: Character): characterList.type = {
    characterList += character
  }

  def removeCharacter(character: Character): characterList.type = {
    characterList -= character
  }

  val characterList: ListBuffer[Character] = ListBuffer[Character]()
  val doorList: ListBuffer[Door] = ListBuffer[Door]()

  def init(): Unit = {

  }

  def render(g: Graphics, doorImage: Image, offsetX: Float, offsetY: Float): Unit = {
    g.setColor(Color.gray)
    g.fillRect(x - offsetX, y - offsetY, w, h)


    doorList.foreach(door => {
      g.drawImage(doorImage, x + door.x - offsetX, y + door.y - offsetY)
    })

    characterList.foreach(character => {
      g.setColor(Color.cyan)
      g.fillRect(x + character.x - offsetX, y + character.y - offsetY, character.w, character.h)
    })

    characterList.foreach(character => {
      g.setColor(Color.darkGray)
      g.drawString(character.name, x + character.x - 10 - offsetX, y + character.y - 25 - offsetY)
    })
  }

  def update(gc: GameContainer, delta: Int): Unit = {
    characterList.foreach(character => {
      character.update(gc, delta)
    })
  }
}
