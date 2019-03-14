package com.kamilkurp.entities

import akka.actor.ActorRef
import com.kamilkurp.ControlScheme.ControlScheme
import com.kamilkurp._
import com.kamilkurp.behaviors.{FollowingBehavior, RelaxedBehavior, RunToExitBehavior}
import org.newdawn.slick.geom.{Polygon, Rectangle, Shape, Transform}
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class Character(val name: String, var room: Room, val controlScheme: ControlScheme, var image: Image) extends Entity {

  override var w: Float = Globals.CHARACTER_SIZE
  override var h: Float = Globals.CHARACTER_SIZE
  override var x: Float = _
  override var y: Float = _
  override var currentVelocityX: Float = 0.0f
  override var currentVelocityY: Float = 0.0f

  var followingBehavior: FollowingBehavior = new FollowingBehavior(this)
  var runToExitBehavior: RunToExitBehavior = new RunToExitBehavior(this)
  var relaxedBehavior: RelaxedBehavior = new RelaxedBehavior(this)

  var walkAngle: Float = 0
  var viewAngle: Float = 0

  var viewRayList: ListBuffer[Shape] = ListBuffer[Shape]()

  for (i <- 0 until 12) {
    var polygon: Shape = new Polygon(new Rectangle(0, 0, 200, 1).getPoints)
    viewRayList += polygon
  }

  var behaviorSet: mutable.HashSet[String] = new mutable.HashSet[String]()
  behaviorSet += "relaxed"

  var controls: (Int, Int, Int, Int) = _

  var speed: Float = 3.0f

  var slow: Float = 0.0f
  var slowTimer: Int = 0

  var lookTimer: Int = 0

  var slowed: Boolean = false

  var actor: ActorRef = _

  var deviationX: Float = 0
  var deviationY: Float = 0

  var shape: Shape = new Polygon()

  var isFree = false
  while (!isFree) {
    x = Random.nextInt(room.w - w.toInt)
    y = Random.nextInt(room.h - h.toInt)

    val collisionDetails = Globals.manageCollisions(room, this)
    if (!collisionDetails.colX || !collisionDetails.colY) {
      isFree = true
    }
  }

  if (Random.nextInt(100) < 100) behaviorSet += "runToExit"

  def this(name: String, room: Room, controlScheme: ControlScheme, controls: (Int, Int, Int, Int), image: Image) {
    this(name, room, controlScheme, image)
    this.controls = controls


  }

  def update(gc: GameContainer, delta: Int): Unit = {
    slowTimer = slowTimer + delta
    lookTimer = lookTimer + delta

    if (controlScheme == ControlScheme.Agent) updateAgent(delta)
    else if (controlScheme == ControlScheme.Manual) updateManual(gc)

    val collisionDetails = Globals.manageCollisions(room, this)
    if (!collisionDetails.colX) {
      this.x += currentVelocityX
    }
    if (!collisionDetails.colY) {
      this.y += currentVelocityY
    }
  }

  private def updateAgent(delta: Int): Unit = {
    if (slowTimer > 3000) {
      slow = 0f
    }

    if (lookTimer > 50) {
      if (viewAngle < walkAngle) {
        if (viewAngle + 8 < walkAngle) viewAngle += 8
        else viewAngle = walkAngle
      }
      else if (viewAngle > walkAngle) {
        if (viewAngle - 8 > walkAngle) viewAngle -= 8
        else viewAngle = walkAngle
      }
      lookTimer = 0
    }


    if (behaviorSet.contains("following")) {
        followingBehavior.perform(delta)
    }
    else if (behaviorSet.contains("runToExit")) {
      runToExitBehavior.perform(delta)
    }
    else if (behaviorSet.contains("relaxed")) {
      relaxedBehavior.perform(delta)
    }
  }

  private def updateManual(gc: GameContainer): Unit = {
    var moved = false

    if (gc.getInput.isKeyDown(controls._1)) {
      currentVelocityX = -speed
      moved = true
    }
    else if (gc.getInput.isKeyDown(controls._2)) {
      currentVelocityX = speed
      moved = true
    }
    else {
      currentVelocityX = 0
    }
    if (gc.getInput.isKeyDown(controls._3)) {
      currentVelocityY = -speed
      moved = true
    }
    else if (gc.getInput.isKeyDown(controls._4)) {
      currentVelocityY = speed
      moved = true
    }
    else {
      currentVelocityY = 0
    }

    if (moved) {
      CameraView.x = room.x + x - Globals.WINDOW_X/Globals.SCALE_X / 2 + w / 2
      CameraView.y = room.y + y - Globals.WINDOW_Y/Globals.SCALE_Y / 2 + h / 2
    }
  }


  override def onCollision(entity: Entity): Unit = {
    //println("this character " + name + " collided with " + entity.name)

    if (entity.getClass == classOf[Character]) {
      slowTimer = 0
      slow = 0.2f
    }
  }

  def changeRoom(newRoom: Room, newX: Float, newY: Float): Unit = {
    room.removeCharacter(this)
    newRoom.addCharacter(this)

    room = newRoom
    x = newX
    y = newY
  }

  def setActor(actor: ActorRef): Unit = {
    this.actor = actor
  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.drawImage(this.image, room.x + this.x - offsetX, room.y + this.y - offsetY)

    this.shape = new Polygon(new Rectangle(x + this.x - offsetX,y + this.y - offsetY,this.w,this.h).getPoints)

    g.setColor(Color.red)
    if (this.behaviorSet.contains("runToExit")) {
      g.fillRect(room.x + this.x - offsetX, room.y + this.y - offsetY, 5, 5)
    }
    //g.drawArc(x + character.x + character.w / 2 - offsetX - 100, y + character.y + character.h / 2 - offsetY - 100, 200, 200, character.viewAngle-60, character.viewAngle+60)



    this.drawViewRays(g, offsetX, offsetY, room.x, room.y)
  }

  def drawViewRays(g: Graphics, offsetX: Float, offsetY: Float, roomX: Float, roomY: Float): Unit = {
    for (i <- viewRayList.indices) {
      var x: Float = roomX + this.x + this.w / 2 - offsetX
      var y: Float = roomY + this.y + this.h / 2 - offsetY

      var polygon: Shape = new Polygon(new Rectangle(x, y, 200, 1).getPoints)

      var t: Transform = Transform.createRotateTransform(Math.toRadians(this.viewAngle - 60 + i* 10).toFloat, x, y)
      polygon = polygon.transform(t)

      viewRayList(i) = polygon
    }

    var col = new Color(Color.green)
    col.a = 0.2f
    for (i <- viewRayList.indices) {
      g.setColor(col)
      g.draw(viewRayList(i))
    }
  }
}
