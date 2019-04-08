package com.kamilkurp.agent

import akka.actor.ActorRef
import com.kamilkurp.behaviors.{FollowBehavior, HoldMeetPointBehavior, IdleBehavior, LeaderBehavior}
import com.kamilkurp.building.{Door, MeetPoint, Room}
import com.kamilkurp.entity.Entity
import com.kamilkurp.utils.ControlScheme.ControlScheme
import com.kamilkurp.utils.{ControlScheme, Globals, Timer}
import org.jgrapht.Graph
import org.jgrapht.graph.{DefaultEdge, SimpleGraph}
import org.newdawn.slick.geom._
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}
import org.newdawn.slick.geom.Shape

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class Agent(val name: String, var room: Room, val controlScheme: ControlScheme, var image: Image, var roomGraph: Graph[Room, DefaultEdge]) extends Entity with BehaviorManager with Follower {

  val rememberedRoute: mutable.Map[String, (Float, Float)] = mutable.Map[String, (Float, Float)]()
  val speed: Float = 0.5f
  val chanceToBeLeader: Float = 0.25f
  override var currentVelocityX: Float = 0.0f
  override var currentVelocityY: Float = 0.0f
  override var shape: Shape = new Rectangle(0, 0, Globals.AGENT_SIZE, Globals.AGENT_SIZE)

  var walkAngle: Float = 0
  var viewAngle: Float = 0

  val viewCone: ViewCone = new ViewCone(this)
  var controls: (Int, Int, Int, Int) = _
  var slow: Float = 0.0f
  val slowTimer: Timer = new Timer(3000)
  val lookTimer: Timer = new Timer(50)
  var actor: ActorRef = _
  var deviationX: Float = 0
  var deviationY: Float = 0
  var atDoor: Boolean = false


  val outOfWayTimer: Timer = new Timer(1000)
  outOfWayTimer.set(outOfWayTimer.timeout)
  var movingOutOfTheWay: Boolean = false


  var isFree = false

  var doorToEnter: Door = _

  while (!isFree) {
    shape.setX(Random.nextInt(room.w - Globals.AGENT_SIZE))
    shape.setY(Random.nextInt(room.h - Globals.AGENT_SIZE))

    val collisionDetails = Globals.manageCollisions(room, this)

    if (!collisionDetails.colX && !collisionDetails.colY) {
      isFree = true
    }
  }

  behaviorManagerInit()

  addRoomToGraph(room)

  def this(name: String, room: Room, controlScheme: ControlScheme, controls: (Int, Int, Int, Int), image: Image, roomGraph: Graph[Room, DefaultEdge]) {
    this(name, room, controlScheme, image, roomGraph)
    this.controls = controls
  }

  def update(gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    slowTimer.update(delta)
    lookTimer.update(delta)

    viewCone.update(delta)

    if (lookTimer.timedOut() && walkAngle != viewAngle) {

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
        val turnSpeed = 12
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

      lookTimer.reset()
    }

    if (slowTimer.timedOut()) {
      slow = 0f
    }

    if (controlScheme == ControlScheme.Agent) {
      getBehavior(currentBehavior).perform(delta)
    }
    else if (controlScheme == ControlScheme.Manual) {
      ControlScheme.handleManualControls(this, gc, delta, renderScale)
    }

    val collisionDetails = Globals.manageCollisions(room, this)
    if (!collisionDetails.colX) {
      shape.setX(shape.getX + currentVelocityX)
    }
    if (!collisionDetails.colY) {
      shape.setY(shape.getY + currentVelocityY)
    }


  }



  override def onCollision(entity: Entity): Unit = {
    if (entity.getClass == classOf[Agent]) {
      slowTimer.reset()
      slow = 0.2f
    }
    if (entity.getClass == classOf[MeetPoint]) {
      shape.setX(1000)
      shape.setY(1000)
    }
  }

  override def changeRoom(entryDoor: Door, newX: Float, newY: Float): Unit = {
    if (controlScheme != ControlScheme.Manual && doorToEnter != entryDoor) return

    if (!roomGraph.containsVertex(entryDoor.leadingToDoor.room)) {
      addRoomToGraph(entryDoor.leadingToDoor.room)
    }


    atDoor = false

    followTimer.start()

    val newRoom: Room = entryDoor.leadingToDoor.room

    if (rememberedRoute.contains(newRoom.name)) {
      followX = rememberedRoute(newRoom.name)._1
      followY = rememberedRoute(newRoom.name)._2
      followDistance = 0
    }
    else {
      followX = newRoom.w / 2
      followY = newRoom.h / 2
      followDistance = 0
    }

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

    getBehavior(currentBehavior).afterChangeRoom()
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
    g.drawString(name, room.x + shape.getX - 10 - offsetX, room.y + shape.getY - 40 - offsetY)
    if (currentBehavior == IdleBehavior.name) g.setColor(Color.cyan)
    if (currentBehavior == FollowBehavior.name && !lostSightOfFollowedEntity) g.setColor(Color.yellow)
    if (currentBehavior == FollowBehavior.name && lostSightOfFollowedEntity) g.setColor(Color.green)
    if (currentBehavior == LeaderBehavior.name) g.setColor(Color.red)
    if (currentBehavior == HoldMeetPointBehavior.name) g.setColor(Color.orange)

    var tag: String = ""
    if (currentBehavior == FollowBehavior.name && followedAgent != null) {
      tag = "[" + currentBehavior + " " + followedAgent.name + "]"
    }
    else {
      tag = "[" + currentBehavior + "]"

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

  def removeRandomRooms(): Unit = {
    val agentRoomGraph = new SimpleGraph[Room, DefaultEdge](classOf[DefaultEdge])

    val toRemove: ListBuffer[Room] = new ListBuffer[Room]()
    val chanceToRemove = 0.25

    // copy graph
    val vertexIter: java.util.Iterator[Room] = roomGraph.vertexSet().iterator()
    while(vertexIter.hasNext) {
      val room = vertexIter.next()
      agentRoomGraph.addVertex(room)

      // fill list with random rooms to remove
      if (Random.nextFloat() < chanceToRemove) {
        toRemove += room
      }
    }

    val edgeIter: java.util.Iterator[DefaultEdge] = roomGraph.edgeSet().iterator()
    while(edgeIter.hasNext) {
      val edge = edgeIter.next()
      agentRoomGraph.addEdge(roomGraph.getEdgeSource(edge), roomGraph.getEdgeTarget(edge))
    }

    // remove random rooms
    for (room <- toRemove) {
      agentRoomGraph.removeVertex(room)
    }

    roomGraph = agentRoomGraph
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
}
