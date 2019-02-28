package com.kamilkurp

import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable.ListBuffer

class Room(val name: String, val x: Int, val y: Int, val w: Int, val h: Int) {
  val characterList: ListBuffer[Character] = ListBuffer[Character]()
  val doorList: ListBuffer[Door] = ListBuffer[Door]()
  var evacuationDoor: Door = _

  def addCharacter(character: Character): characterList.type = {
    characterList += character
  }

  def removeCharacter(character: Character): characterList.type = {
    characterList -= character
  }

  def init(): Unit = {

  }

  def render(g: Graphics, doorImage: Image, offsetX: Float, offsetY: Float): Unit = {
    g.setColor(Color.gray)
    g.fillRect(x - offsetX, y - offsetY, w, h)


    doorList.foreach(door => {
      g.drawImage(door.image, x + door.x - offsetX, y + door.y - offsetY)
    })

    characterList.foreach(character => {
      g.drawImage(character.image, x + character.x - offsetX, y + character.y - offsetY)
    })

    characterList.foreach(character => {
      g.setColor(Color.darkGray)
      g.drawString(character.name, x + character.x - 10 - offsetX, y + character.y - 25 - offsetY)
    })

    for (character1 <- characterList) {
      for (character2 <- characterList) {
        if (Math.abs(character1.x - character2.x) <= 50
          && Math.abs(character1.y - character2.y) <= 50
          && character1 != character2 && character1.name != "Player" && character2.name != "Player") {
          character1.actor ! SomeoneNearby(character2.name, character2.x, character2.y, character2.w, character2.h)
        }
      }
    }

  }

  def update(gc: GameContainer, delta: Int): Unit = {
    characterList.foreach(character => {
      character.update(gc, delta)
    })
  }
}
