package com.kamilkurp

import org.newdawn.slick.{Color, Graphics}
import org.newdawn.slick.geom.{Polygon, Rectangle, Shape, Transform}

import scala.collection.mutable.ListBuffer

import com.kamilkurp.entities.Character

class ViewCone(character: Character) {
  var firstRay: (Rectangle, Float) = (new Rectangle(0,0,0,0), 0)
  var lastRay: (Rectangle, Float) = (new Rectangle(0,0,0,0), 0)

  var viewRayList: ListBuffer[Shape] = ListBuffer[Shape]()


  for (_ <- 0 until 24) {
    var polygon: Shape = new Polygon(new Rectangle(0, 0, 200, 1).getPoints)
    viewRayList += polygon
  }


  def update(delta: Float) {

    val x: Float = character.shape.getX + character.shape.getWidth / 2
    val y: Float = character.shape.getY + character.shape.getHeight / 2

    firstRay = (new Rectangle(0,0,0,0), 0)
    lastRay = (new Rectangle(0,0,0,0), 0)


    for (i <- viewRayList.indices) {
      val rect = new Rectangle(x, y, 1200, 1)
      var polygon: Shape = new Polygon(rect.getPoints)

      val radianAngle = character.viewAngle - 60 + i * 5
      val t: Transform = Transform.createRotateTransform(Math.toRadians(radianAngle).toFloat, x, y)
      polygon = polygon.transform(t)

      viewRayList(i) = polygon

      if(i == 0) {
        firstRay = (rect, radianAngle)
      }
      if(i == viewRayList.length - 1) {
        lastRay = (rect, radianAngle)
      }
    }

    firstRay._1.setWidth(100)
    lastRay._1.setWidth(100)

    character.room.characterList.filter(c => c != character).foreach(that =>
      viewRayList.foreach(rayShape =>
        if (that.shape.intersects(rayShape)) {
          character.actor ! CharacterWithinVision(that, character.getDistanceTo(that), delta)
        }
      )
    )
  }



  def draw(g: Graphics, offsetX: Float, offsetY: Float) = {

    val x: Float = character.shape.getX + character.shape.getWidth / 2
    val y: Float = character.shape.getY + character.shape.getHeight / 2

    val col = new Color(Color.green)
    col.a = 1f

    val t: Transform = Transform.createTranslateTransform(character.room.x - offsetX, character.room.y - offsetY)
    var polygon1: Shape = new Polygon(firstRay._1.getPoints)
    val firstRotation: Transform = Transform.createRotateTransform(Math.toRadians(firstRay._2).toFloat, x, y)
    polygon1 = polygon1.transform(firstRotation)
    polygon1 = polygon1.transform(t)
    var polygon2: Shape = new Polygon(lastRay._1.getPoints)
    val lastRotation: Transform = Transform.createRotateTransform(Math.toRadians(lastRay._2).toFloat, x, y)
    polygon2 = polygon2.transform(lastRotation)
    polygon2 = polygon2.transform(t)

    g.setColor(col)
    g.draw(polygon1)
    g.draw(polygon2)
  }

}
