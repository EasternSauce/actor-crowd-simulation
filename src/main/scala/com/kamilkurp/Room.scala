package com.kamilkurp

import com.kamilkurp.entities.Door
import org.newdawn.slick.geom._
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable.ListBuffer

class Room(val name: String, val x: Int, val y: Int, val w: Int, val h: Int) {
  val characterList: ListBuffer[entities.Character] = ListBuffer[entities.Character]()
  val doorList: ListBuffer[Door] = ListBuffer[Door]()
  var evacuationDoor: Door = _

  var shape: Shape = _


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
      g.drawImage(door.image, x + door.x - offsetX, y + door.y - offsetY)
    })

    characterList.foreach(character => {
      g.drawImage(character.image, x + character.x - offsetX, y + character.y - offsetY)

      character.shape = new Polygon(new Rectangle(x + character.x - offsetX,y + character.y - offsetY,character.w,character.h).getPoints)

      g.setColor(Color.red)
      if (character.behaviorSet.contains("runToExit")) {
        g.fillRect(x + character.x - offsetX, y + character.y - offsetY, 5, 5)
      }
      //g.drawArc(x + character.x + character.w / 2 - offsetX - 100, y + character.y + character.h / 2 - offsetY - 100, 200, 200, character.viewAngle-60, character.viewAngle+60)



      character.drawViewRays(g, offsetX, offsetY, this.x, this.y)
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

    for (character1 <- characterList) {
      if (character1.name eq "Alfonso") {
        val viewRayList = character1.viewRayList
        for (character2 <- characterList) {
          if (character1 != character2) {
            for (rayShape <- viewRayList) {
              if (character2.shape.intersects(rayShape)) {
                println(character1.name + " sees " + character2.name)
              }
            }
          }

        }
      }
    }

    for (character1 <- characterList) {
      for (character2 <- characterList) {
        if (Math.abs(character1.x - character2.x) <= 50
          && Math.abs(character1.y - character2.y) <= 50
          && character1 != character2 && character1.name != "Player" && character2.name != "Player") {
          character1.actor ! SomeoneNearby(character2.name, character2.x, character2.y, character2.w, character2.h)
        }

        if (Math.abs(character1.x - character2.x) <= 70
          && Math.abs(character1.y - character2.y) <= 70
          && character1 != character2 && character1.name != "Player" && character2.name != "Player"
          && !character1.behaviorSet.contains("runToExit") && character2.behaviorSet.contains("runToExit")) {
          character1.actor ! OutOfTheWay(character2.name, character2.x, character2.y, character2.w, character2.h)
        }

        if (Math.abs(character1.x - character2.x) <= 400
          && Math.abs(character1.y - character2.y) <= 400
          && character1 != character2 && character1.name != "Player" && character2.name != "Player"
          && !character1.behaviorSet.contains("runToExit") && character2.behaviorSet.contains("runToExit")
          && character1.room == character2.room) {
          character1.actor ! SomeoneEvacuating(character2.name, character2.x, character2.y, character2.w, character2.h)
        }
      }
    }

  }

}
