package com.kamilkurp.building

import com.kamilkurp.agent.Agent
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable.ListBuffer

class Room(val name: String, val x: Int, val y: Int, val w: Int, val h: Int) {
  val agentList: ListBuffer[Agent] = ListBuffer[Agent]()
  val doorList: ListBuffer[Door] = ListBuffer[Door]()
  val meetPointList: ListBuffer[MeetPoint] = ListBuffer[MeetPoint]()

  var evacuationDoor: Door = _

  def addCharacter(character: Agent): agentList.type = {
    agentList += character
  }

  def removeCharacter(character: Agent): agentList.type = {
    agentList -= character
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

    agentList.foreach(character => {
      character.draw(g, offsetX, offsetY)
    })

    agentList.foreach(character => {
      character.drawName(g, offsetX, offsetY)
    })


  }

  def update(gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    agentList.foreach(character => {
      character.update(gc, delta, renderScale)
    })


  }

}
