package com.kamilkurp

import com.kamilkurp.entities.{Door, MeetPoint}
import org.newdawn.slick.geom._
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
    g.setColor(Color.gray)
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
      character.drawName(g, offsetX,offsetY)
    })



  }

  def update(gc: GameContainer, delta: Int): Unit = {
    characterList.foreach(character => {
      character.update(gc, delta)
    })

//    for (character1 <- characterList) {
//      for (character2 <- characterList) {
//        if (Math.abs(character1.shape.getX - character2.shape.getX) <= 50
//          && Math.abs(character1.shape.getY - character2.shape.getY) <= 50
//          && character1 != character2 && character1.name != "Player" && character2.name != "Player") {
//          character1.actor ! SomeoneNearby(character2.name, character2.shape.getX, character2.shape.getY, character2.shape.getWidth, character2.shape.getHeight)
//        }
//
//        if (Math.abs(character1.shape.getX - character2.shape.getX) <= 70
//          && Math.abs(character1.shape.getY - character2.shape.getY) <= 70
//          && character1 != character2 && character1.name != "Player" && character2.name != "Player"
//          && character1.currentBehavior != "runToExit" && character2.currentBehavior == "runToExit") {
//          character1.actor ! OutOfTheWay(character2.name, character2.shape.getX, character2.shape.getY, character2.shape.getWidth, character2.shape.getHeight)
//        }

//        if (Math.abs(character1.shape.getX - character2.shape.getX) <= 400
//          && Math.abs(character1.shape.getY - character2.shape.getY) <= 400
//          && character1 != character2 && character1.name != "Player" && character2.name != "Player"
//          && character1.currentBehavior != "runToExit" && character2.currentBehavior == "runToExit"
//          && character1.room == character2.room) {
//          character1.actor ! SomeoneEvacuating(character2.name, character2.shape.getX, character2.shape.getY, character2.shape.getWidth, character2.shape.getHeight)
//        }
//      }
//    }

  }

}
