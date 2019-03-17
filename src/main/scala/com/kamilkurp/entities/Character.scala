package com.kamilkurp.entities

import akka.actor.ActorRef
import com.kamilkurp.ControlScheme.ControlScheme
import com.kamilkurp._
import com.kamilkurp.behaviors.{Behavior, FollowingBehavior, RelaxedBehavior, RunToExitBehavior}
import org.newdawn.slick.geom._
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable.ListBuffer
import scala.util.Random

class Character(val name: String, var room: Room, val controlScheme: ControlScheme, var image: Image) extends Entity {
  override var currentVelocityX: Float = 0.0f
  override var currentVelocityY: Float = 0.0f
  override var shape: Shape = new Rectangle(0, 0, Globals.CHARACTER_SIZE, Globals.CHARACTER_SIZE)

  var currentBehavior: String = _

  var walkAngle: Float = 0
  var viewAngle: Float = 0

  var viewRayList: ListBuffer[Shape] = ListBuffer[Shape]()

  for (_ <- 0 until 12) {
    var polygon: Shape = new Polygon(new Rectangle(0, 0, 200, 1).getPoints)
    viewRayList += polygon
  }

  currentBehavior = "relaxed"

  var controls: (Int, Int, Int, Int) = _

  var speed: Float = 3.0f

  var slow: Float = 0.0f
  var slowTimer: Int = 0

  var lookTimer: Int = 0

  var slowed: Boolean = false

  var actor: ActorRef = _

  var deviationX: Float = 0
  var deviationY: Float = 0

  val chanceToBeLeader: Float = 100

  var isFree = false
  while (!isFree) {
    shape.setX(Random.nextInt(room.w - Globals.CHARACTER_SIZE))
    shape.setY(Random.nextInt(room.w - Globals.CHARACTER_SIZE))

    val collisionDetails = Globals.manageCollisions(room, this)
    if (!collisionDetails.colX || !collisionDetails.colY) {
      isFree = true
    }

  }

  if (Random.nextInt(100) < chanceToBeLeader) {
    currentBehavior = "runToExit"
  }

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
      shape.setX(shape.getX + currentVelocityX)
    }
    if (!collisionDetails.colY) {
      shape.setY(shape.getY + currentVelocityY)
    }

    if (name eq "Player") {
      room.characterList.filter(c => c != this).foreach(character =>
        viewRayList.foreach(rayShape =>
          if (character.shape.intersects(rayShape)) {
            println(name + " sees " + character.name + " at distance " + getDistanceTo(character))
          }
        )
      )
    }
  }

  private def updateAgent(delta: Int): Unit = {
    if (slowTimer > 3000) {
      slow = 0f
    }

    if (lookTimer > 50 && walkAngle != viewAngle) {
      adjustViewAngle(Character.findSideToTurn(viewAngle, walkAngle))

      lookTimer = 0
    }

    Behavior.getBehavior(currentBehavior).perform(this, delta)
  }

  private def updateManual(gc: GameContainer): Unit = {
    var moved = false

    if (lookTimer > 50 && walkAngle != viewAngle) {
      adjustViewAngle(Character.findSideToTurn(viewAngle, walkAngle))
      lookTimer = 0
    }

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

    if (currentVelocityX != 0 || currentVelocityY != 0) {
      val normalVector = new Vector2f(currentVelocityX, currentVelocityY)
      normalVector.normalise()

      walkAngle = normalVector.getTheta.floatValue()
    }

    if (moved) {
      CameraView.x = room.x + shape.getX - Globals.WINDOW_X/Globals.SCALE_X / 2 + shape.getWidth / 2
      CameraView.y = room.y + shape.getY - Globals.WINDOW_Y/Globals.SCALE_Y / 2 + shape.getHeight / 2
    }
  }


  private def adjustViewAngle(clockwise: Boolean) = {
    if (Math.abs(viewAngle - walkAngle) > 6 && Math.abs((viewAngle + 180) % 360 - (walkAngle + 180) % 360) > 6) {
      if (clockwise) { // clockwise
        if (viewAngle + 6 < 360) viewAngle += 6
        else viewAngle = viewAngle + 6 - 360
      }
      else { // counterclockwise
        if (viewAngle - 6 > 0) viewAngle -= 6
        else viewAngle = viewAngle - 6 + 360
      }
    }
    else {
      viewAngle = walkAngle
    }
  }

  override def onCollision(entity: Entity): Unit = {
    if (entity.getClass == classOf[Character]) {
      slowTimer = 0
      slow = 0.2f
    }
  }

  def changeRoom(newRoom: Room, newX: Float, newY: Float): Unit = {
    room.removeCharacter(this)
    newRoom.addCharacter(this)

    room = newRoom
    shape.setX(newX)
    shape.setY(newY)
  }

  def setActor(actor: ActorRef): Unit = {
    this.actor = actor
  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.drawImage(image, room.x + shape.getX - offsetX, room.y + shape.getY - offsetY)

    g.setColor(Color.red)
    if (currentBehavior == "runToExit") {
      g.fillRect(room.x + shape.getX - offsetX, room.y + shape.getY - offsetY, 5, 5)
    }

    drawViewRays(g, offsetX, offsetY, room.x, room.y)
  }

  def drawName(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.setColor(Color.darkGray)
    g.drawString(name, room.x + shape.getX - 10 - offsetX, room.y + shape.getY - 25 - offsetY)
  }

  private def drawViewRays(g: Graphics, offsetX: Float, offsetY: Float, roomX: Float, roomY: Float): Unit = {
    for (i <- viewRayList.indices) {
      val x: Float = shape.getX + shape.getWidth / 2
      val y: Float = shape.getY + shape.getHeight / 2

      var polygon: Shape = new Polygon(new Rectangle(x, y, 200, 1).getPoints)

      val t: Transform = Transform.createRotateTransform(Math.toRadians(this.viewAngle - 60 + i* 10).toFloat, x, y)
      polygon = polygon.transform(t)

      viewRayList(i) = polygon
    }

    val col = new Color(Color.green)
    col.a = 1f
    for (i <- viewRayList.indices) {
      g.setColor(col)

      var polygon: Shape = new Polygon(viewRayList(i).getPoints)
      val t: Transform = Transform.createTranslateTransform(roomX - offsetX, roomY - offsetY)
      polygon = polygon.transform(t)


      g.draw(polygon)
    }
  }


  def getDistanceTo(entity: Entity): Float = {
    val differenceSquaredX = Math.pow(entity.shape.getCenterX.doubleValue() - shape.getCenterX.doubleValue(),2)
    val differenceSquaredY = Math.pow(entity.shape.getCenterY.doubleValue() - shape.getCenterY.doubleValue(),2)
    Math.sqrt(differenceSquaredX + differenceSquaredY).floatValue()
  }
}


object Character {
  def findSideToTurn(currentAngle: Float, desiredAngle: Float): Boolean = {
    var clockwise = false
    if (currentAngle < 180) {
      if (desiredAngle - currentAngle >= 0 && desiredAngle - currentAngle < 180) clockwise = true
      else clockwise = false
    }
    else {
      if (currentAngle - desiredAngle >= 0 && currentAngle - desiredAngle < 180) clockwise = false
      else clockwise = true

    }
    clockwise
  }

}