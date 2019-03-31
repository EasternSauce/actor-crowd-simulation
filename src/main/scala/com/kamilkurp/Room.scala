package com.kamilkurp

import com.kamilkurp.entities.{Door, MeetPoint}
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable.ListBuffer

class Room(val name: String, val x: Int, val y: Int, val w: Int, val h: Int) {
  val characterList: ListBuffer[entities.Character] = ListBuffer[entities.Character]()
  val doorList: ListBuffer[Door] = ListBuffer[Door]()
  val meetPointList: ListBuffer[MeetPoint] = ListBuffer[MeetPoint]()

  var evacuationDoor: Door = _

  def addCharacter(character: entities.Character): characterList.type = {
    characterList += character
  }

  def removeCharacter(character: entities.Character): characterList.type = {
    characterList -= character
  }

  def init(): Unit = {

  }

  def render(g: Graphics, doorImage: Image, offsetX: Float, offsetY: Float): Unit = {
    if (name.startsWith("corr")) g.setColor(Color.darkGray)
    else if (name.startsWith("room")) g.setColor(Color.lightGray)




    g.fillRect(x - offsetX, y - offsetY, w, h)


    doorList.foreach(door => {
      door.draw(g, offsetX, offsetY)
    })

    meetPointList.foreach(meetPoint => {
      meetPoint.draw(g, offsetX, offsetY)
    })

    characterList.foreach(character => {
      character.draw(g, offsetX, offsetY)
    })

    characterList.foreach(character => {
      character.drawName(g, offsetX, offsetY)
    })


  }

  def update(gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    characterList.foreach(character => {
      character.update(gc, delta, renderScale)
    })


  }

}
