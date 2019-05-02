package com.kamilkurp.agent

import com.kamilkurp.util.{Configuration, Timer}
import org.newdawn.slick.geom.{Polygon, Rectangle, Shape, Transform}
import org.newdawn.slick.{Color, Graphics}

import scala.collection.mutable.ListBuffer
import scala.util.Random

class VisionModule private() {

  var colAvoidAngle: Float = _
  private var agent: Agent = _
  private var firstRay: (Rectangle, Float) = _
  private var lastRay: (Rectangle, Float) = _
  private var visionTimer: Timer = _
  private var viewRayList: ListBuffer[Shape] = _
  private var viewRayColorList: ListBuffer[Color] = _
  private var drawRays: Boolean = _

  def update(delta: Int) {

    val x: Float = agent.shape.getX + agent.shape.getWidth / 2
    val y: Float = agent.shape.getY + agent.shape.getHeight / 2

    firstRay = (new Rectangle(0, 0, 0, 0), 0)
    lastRay = (new Rectangle(0, 0, 0, 0), 0)


    for (i <- viewRayList.indices) {
      val rect = new Rectangle(x, y, 1200, 1)
      var polygon: Shape = new Polygon(rect.getPoints)

      val radianAngle = agent.movementModule.viewAngle - 60 + i * 5
      val t: Transform = Transform.createRotateTransform(Math.toRadians(radianAngle).toFloat, x, y)
      polygon = polygon.transform(t)

      viewRayList(i) = polygon

      if (i == 0) {
        firstRay = (rect, radianAngle)
      }
      if (i == viewRayList.length - 1) {
        lastRay = (rect, radianAngle)
      }
    }

    firstRay._1.setWidth(100)
    lastRay._1.setWidth(100)

    if (!visionTimer.timedOut()) {
      return
    }
    visionTimer.reset()


    for (i <- 0 until 24) {
      viewRayColorList(i) = Color.green
    }

    agent.currentRoom.agentList.filter(c => c != agent).foreach(that => {
      for (i <- viewRayList.indices) {
        if (that.shape.intersects(viewRayList(i))) {
          agent.actor ! AgentWithinVision(that)

          if (agent.getDistanceTo(that) < 120) {
            viewRayColorList(i) = Color.red
          }
        }
      }
    })

    agent.currentRoom.flamesList.foreach(that => {
      for (i <- viewRayList.indices) {
        if (that.shape.intersects(viewRayList(i))) {
          if (agent.getDistanceTo(that) < 120) {
            viewRayColorList(i) = Color.red
          }
        }
      }
    })


    var largestClusterPos = -1
    var largestClusterSize = -1
    var currentClusterPos = -1
    var currentClusterSize = -1
    var inCluster = false
    for (i <- viewRayColorList.indices) {
      if (viewRayColorList(i) == Color.green) {
        if (!inCluster) {
          currentClusterPos = i
          currentClusterSize = 1
          inCluster = true
        }
        else {
          currentClusterSize = currentClusterSize + 1
        }

        if (currentClusterSize > largestClusterSize) {
          largestClusterPos = currentClusterPos
          largestClusterSize = currentClusterSize
        }
      }
      if (viewRayColorList(i) == Color.red) {
        if (inCluster) {
          inCluster = false
        }
      }
    }

    if (viewRayColorList(11) == Color.green && viewRayColorList(12) == Color.green) {
      colAvoidAngle = 0
    }
    else {
      colAvoidAngle = 60 - 5 * (largestClusterPos + largestClusterSize)
    }

    if (largestClusterPos != -1) {
      if (agent.name == "agent6") println(largestClusterPos + " " + largestClusterSize)
    }


    agent.currentRoom.flamesList.foreach(fire =>
      viewRayList.foreach(rayShape =>
        if (fire.shape.intersects(rayShape)) {
          if (agent.avoidFireTimer.timedOut()) {
            agent.avoidFireTimer.reset()
            agent.avoidFireTimer.start()
            agent.actor ! FireWithinVision()
          }
        }
      )
    )
  }


  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {

    val x: Float = agent.shape.getX + agent.shape.getWidth / 2
    val y: Float = agent.shape.getY + agent.shape.getHeight / 2

    val col = new Color(Color.green)
    col.a = 1f

    if (!drawRays) {
      val t: Transform = Transform.createTranslateTransform(agent.currentRoom.x - offsetX, agent.currentRoom.y - offsetY)
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
    else {
      var i = 0
      viewRayList.foreach(viewRay => {
        g.setColor(viewRayColorList(i))
        i = i + 1
        val t: Transform = Transform.createTranslateTransform(agent.currentRoom.x - offsetX, agent.currentRoom.y - offsetY)
        var polygon1: Shape = new Polygon(viewRay.getPoints)
        polygon1 = polygon1.transform(t)
        g.draw(polygon1)
      })
    }


  }

}


object VisionModule {
  def apply(agent: Agent): VisionModule = {
    val visionModule = new VisionModule

    visionModule.agent = agent

    visionModule.firstRay = (new Rectangle(0, 0, 0, 0), 0)
    visionModule.lastRay = (new Rectangle(0, 0, 0, 0), 0)

    visionModule.visionTimer = new Timer(Configuration.AGENT_VISION_TIMER + Random.nextInt(300) - 150)
    visionModule.visionTimer.start()

    visionModule.viewRayList = ListBuffer[Shape]()

    visionModule.viewRayColorList = ListBuffer[Color]()

    visionModule.drawRays = false

    visionModule.colAvoidAngle = 0.0f

    for (_ <- 0 until 24) {
      var polygon: Shape = new Polygon(new Rectangle(0, 0, 200, 1).getPoints)
      visionModule.viewRayList += polygon
      visionModule.viewRayColorList += Color.green
    }

    visionModule
  }
}