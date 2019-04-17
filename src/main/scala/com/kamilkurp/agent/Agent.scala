package com.kamilkurp.agent

import akka.actor.ActorRef
import com.kamilkurp.behavior._
import com.kamilkurp.building.{Door, MeetPoint, Room}
import com.kamilkurp.entity.Entity
import com.kamilkurp.util.ControlScheme.ControlScheme
import com.kamilkurp.util.{Configuration, ControlScheme, Globals, Timer}
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.newdawn.slick.geom.{Shape, _}
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.util.Random

class Agent(var name: String, var room: Room, val controlScheme: ControlScheme, var image: Image, var roomGraph: Graph[Room, DefaultEdge]) extends Entity {

  override var currentVelocityX: Float = _
  override var currentVelocityY: Float = _
  override var shape: Shape = _
  var walkAngle: Float = _
  var viewAngle: Float = _
  var viewCone: AgentViewCone = _
  var controls: (Int, Int, Int, Int) = _
  var slow: Float = _
  var beingPushed: Boolean = _
  var pushedTimer: Timer = _
  var actor: ActorRef = _
  var atDoor: Boolean = _
  var slowTimer: Timer = _
  var lookTimer: Timer = _
  var outOfWayTimer: Timer = _
  var checkProgressTimer: Timer = _
  var movingOutOfTheWay: Boolean = _
  var isFree: Boolean = _
  var doorToEnter: Door = _
  var pastPositionX: Float = _
  var pastPositionY: Float = _
  var goAroundObstacle: Boolean = _
  var goAroundAngle: Float = _
  var debug: Boolean = _
  var goTowardsDoor: Boolean = _
  var changedVelocityX: Float = _
  var changedVelocityY: Float = _
  var changedVelocity: Boolean = _
  var followManager: FollowManager = _
  var behaviorManager: BehaviorManager = _

  def this(name: String, room: Room, controlScheme: ControlScheme, controls: (Int, Int, Int, Int), image: Image, roomGraph: Graph[Room, DefaultEdge]) {
    this(name, room, controlScheme, image, roomGraph)
    this.controls = controls
  }

  def init(): Unit = {
    currentVelocityX = 0.0f
    currentVelocityY = 0.0f
    shape = new Rectangle(0, 0, Globals.AGENT_SIZE, Globals.AGENT_SIZE)

    walkAngle = 0.0f
    viewAngle = 0.0f

    behaviorManager = new BehaviorManager()
    behaviorManager.init(this)

    viewCone = new AgentViewCone(this)
    slow = 0.0f
    beingPushed = false

    pushedTimer = new Timer(500)
    atDoor = false

    slowTimer = new Timer(Configuration.AGENT_SLOW_TIMER)
    lookTimer = new Timer(Configuration.AGENT_LOOK_TIMER)
    outOfWayTimer = new Timer(Configuration.AGENT_MOVE_OUT_OF_WAY_TIMER)
    checkProgressTimer = new Timer(2000)
    slowTimer.start()
    lookTimer.start()
    outOfWayTimer.start()
    checkProgressTimer.start()

    movingOutOfTheWay = false

    isFree = false

    pastPositionX = 0
    pastPositionY = 0
    goAroundObstacle = false
    goAroundAngle = 0

    debug = false

    goTowardsDoor = false

    changedVelocityX = 0.0f
    changedVelocityY = 0.0f
    changedVelocity = false

    followManager = new FollowManager()

    while (!isFree) {
      shape.setX(Random.nextInt(room.w - Globals.AGENT_SIZE))
      shape.setY(Random.nextInt(room.h - Globals.AGENT_SIZE))

      val collisionDetails = Globals.manageCollisions(room, this, 0, 0)

      if (!collisionDetails.colX && !collisionDetails.colY) {
        isFree = true
      }
    }


    addRoomToGraph(room)
  }

  def update(gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    viewCone.update(delta)

    if (changedVelocity) {
      changedVelocity = false
      currentVelocityX = changedVelocityX
      currentVelocityY = changedVelocityY
    }

    if (checkProgressTimer.timedOut()) {
      checkProgressTimer.reset()

      val progress = getDistanceTo(pastPositionX, pastPositionY)


      if (progress < 60) {
        goAroundObstacle = true
        goAroundAngle = Random.nextInt(60) - 30
      }
      else goAroundObstacle = false

      pastPositionX = shape.getX
      pastPositionY = shape.getY
    }

    if (lookTimer.timedOut() && walkAngle != viewAngle) {

      lookTimer.reset()

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


      def adjustViewAngle(clockwise: Boolean): Unit = {
        val turnSpeed = Configuration.AGENT_TURN_SPEED
        if (Math.abs(viewAngle - walkAngle) > turnSpeed && Math.abs((viewAngle + 180) % 360 - (walkAngle + 180) % 360) > turnSpeed) {
          if (clockwise) { // clockwise
            if (viewAngle + turnSpeed < 360) viewAngle += turnSpeed
            else viewAngle = viewAngle + turnSpeed - 360
          }
          else { // counterclockwise
            if (viewAngle - turnSpeed > 0) viewAngle -= turnSpeed
            else viewAngle = viewAngle - turnSpeed + 360
          }
        }
        else {
          viewAngle = walkAngle
        }
      }

      adjustViewAngle(findSideToTurn(viewAngle, walkAngle))

    }

    if (slowTimer.timedOut()) {
      slow = 0f
      slowTimer.stop()
    }

    if (pushedTimer.timedOut()) {
      beingPushed = false
      pushedTimer.stop()
    }

    if (controlScheme == ControlScheme.Agent) {
      behaviorManager.getBehavior(behaviorManager.currentBehavior).perform(delta)
    }
    else if (controlScheme == ControlScheme.Manual) {
      ControlScheme.handleManualControls(this, gc, delta, renderScale)
    }

    val collisionVelocityX = currentVelocityX * delta
    val collisionVelocityY = currentVelocityY * delta
    val collisionDetails = Globals.manageCollisions(room, this, collisionVelocityX, collisionVelocityY)
    if (!collisionDetails.colX) {
      shape.setX(shape.getX + collisionVelocityX)
    }
    if (!collisionDetails.colY) {
      shape.setY(shape.getY + collisionVelocityY)
    }


  }



  override def onCollision(entity: Entity): Unit = {
    if (entity.getClass == classOf[Agent]) {

      val agent: Agent = entity.asInstanceOf[Agent]

      if (debug) {
        println("collision with " + agent.name)
      }

      pushBack(this, agent)


    }

    //temporary solution, move evacuated outside map
    if (entity.getClass == classOf[MeetPoint]) {
      shape.setX(1000)
      shape.setY(1000)
      room.agentList -= this
    }
  }

  def pushBack(pusher: Agent, pushed: Agent): Unit = {
    if (pushed.behaviorManager.currentBehavior == FollowBehavior.name || pushed.behaviorManager.currentBehavior == SearchExitBehavior.name) {
      if (pusher.behaviorManager.currentBehavior == LeaderBehavior.name) {
        if (!pushed.beingPushed) {
          val vector = new Vector2f(pusher.currentVelocityX, pusher.currentVelocityY)

          vector.setTheta(vector.getTheta + Random.nextInt(30) - 15)

          pushed.changedVelocityX = vector.x
          pushed.changedVelocityY = vector.y
          pushed.changedVelocity = true

          pushed.walkAngle = vector.getTheta.toFloat
          pushed.viewAngle = vector.getTheta.toFloat

          pushed.beingPushed = true
          pushed.pushedTimer.reset()
          pushed.pushedTimer.start()
        }

      }
      else if ((pusher.behaviorManager.currentBehavior == FollowBehavior.name || pushed.behaviorManager.currentBehavior == SearchExitBehavior.name) && pusher.beingPushed) {
        if (!pushed.beingPushed) {
          val vector = new Vector2f(currentVelocityX, currentVelocityY)

          vector.setTheta(vector.getTheta + Random.nextInt(30) - 15)

          pushed.changedVelocityX = vector.x
          pushed.changedVelocityY = vector.y
          pushed.changedVelocity = true

          pushed.walkAngle = vector.getTheta.toFloat
          pushed.viewAngle = vector.getTheta.toFloat

          pushed.beingPushed = true
          pushed.pushedTimer.time = pusher.pushedTimer.time
          pushed.pushedTimer.start()
        }

      }
    }
  }

  override def changeRoom(entryDoor: Door, newX: Float, newY: Float): Unit = {
    if (controlScheme != ControlScheme.Manual && doorToEnter != entryDoor) return

    if (!roomGraph.containsVertex(entryDoor.leadingToDoor.room)) {
      addRoomToGraph(entryDoor.leadingToDoor.room)
    }


    atDoor = false

    followManager.followTimer.reset()

    val newRoom: Room = entryDoor.leadingToDoor.room

    for (agent <- room.agentList) {
      if (agent != this) {
        agent.actor ! AgentEnteredDoor(this, entryDoor, entryDoor.shape.getX, entryDoor.shape.getY)
      }
    }

    room.removeAgent(this)
    newRoom.addAgent(this)

    room = newRoom
    shape.setX(newX)
    shape.setY(newY)

    goTowardsDoor = false

    behaviorManager.getBehavior(behaviorManager.currentBehavior).afterChangeRoom()
  }

  def setActor(actor: ActorRef): Unit = {
    this.actor = actor
  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.drawImage(image, room.x + shape.getX - offsetX, room.y + shape.getY - offsetY)

    viewCone.update(15) // workaround - otherwise cone not drawn properly
    viewCone.draw(g, offsetX, offsetY)
  }

  def drawName(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.setColor(Color.pink)
    if(beingPushed) {
      g.setColor(Color.blue)
    }
    g.drawString(name, room.x + shape.getX - 10 - offsetX, room.y + shape.getY - 40 - offsetY)
    g.setColor(behaviorManager.getBehavior(behaviorManager.currentBehavior).color)

    var tag: String = ""
    if (behaviorManager.currentBehavior == FollowBehavior.name && followManager.followedAgent != null) {
      tag = "[" + behaviorManager.currentBehavior + " " + followManager.followedAgent.name + "]"
    }
    else {
      tag = "[" + behaviorManager.currentBehavior + "]"

    }
    g.drawString(tag, room.x + shape.getX - 10 - offsetX, room.y + shape.getY - 25 - offsetY)

  }

  def addRoomToGraph(room: Room): Unit = {
    roomGraph.addVertex(room)

    for (door <- room.doorList) {
      if (roomGraph.containsVertex(door.leadingToDoor.room)) {
        roomGraph.addEdge(room, door.leadingToDoor.room)
      }
    }
  }


  def findDoorToEnterNext(): Door = {

    var meetPointRoom: Room = null

    val it: java.util.Iterator[Room] = roomGraph.vertexSet().iterator()
    while(it.hasNext) {
      val room = it.next()
      if (room.meetPointList.nonEmpty) meetPointRoom = room
    }

    if (!roomGraph.containsVertex(meetPointRoom)) {
      return null
    }

    import org.jgrapht.alg.shortestpath.DijkstraShortestPath
    val dijkstraShortestPath = new DijkstraShortestPath(roomGraph)

    var shortestPath: java.util.List[Room] = null
    try {
      shortestPath = dijkstraShortestPath.getPath(room, meetPointRoom).getVertexList

    }
    catch {
      case _: NullPointerException =>
        return null
    }

    for (door: Door <- room.doorList) {
      if (shortestPath.size() > 1 && door.leadingToDoor.room == shortestPath.get(1)) {
        return door
      }
    }

    null
  }

  def moveTowards(x: Float, y: Float, delta: Int): Unit = {
    goTo(x, y, delta)
  }

  def moveTowards(entity: Entity, delta: Int): Unit = {
    goTo(entity.shape.getCenterX, entity.shape.getCenterY, delta)
  }

  private def goTo(x: Float, y: Float, delta: Int): Unit = {
    val vector = new Vector2f(x - shape.getCenterX, y - shape.getCenterY)
    vector.normalise()

    if (goAroundObstacle) {
      vector.setTheta(vector.getTheta + goAroundAngle)
    }

    walkAngle = vector.getTheta.floatValue()

    if (!beingPushed) {
      currentVelocityX = vector.x * Configuration.AGENT_SPEED * (1f - slow) * delta
      currentVelocityY = vector.y * Configuration.AGENT_SPEED * (1f - slow) * delta
    }

  }

  def stopMoving(): Unit = {
    currentVelocityX = 0
    currentVelocityY = 0
  }
}
