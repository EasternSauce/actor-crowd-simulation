package com.kamilkurp.agent

import akka.actor.ActorRef
import com.kamilkurp.behavior._
import com.kamilkurp.building.{Door, MeetPoint, Room}
import com.kamilkurp.entity.Entity
import com.kamilkurp.stats.Statistics
import com.kamilkurp.util.ControlScheme.ControlScheme
import com.kamilkurp.util.{Configuration, ControlScheme, Globals, Timer}
import org.jgrapht.Graph
import org.jgrapht.graph.{DefaultEdge, DefaultWeightedEdge, SimpleGraph, SimpleWeightedGraph}
import org.newdawn.slick.geom.{Shape, _}
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

class Agent(var name: String, var room: Room, val controlScheme: ControlScheme, var image: Image, var roomGraph: SimpleGraph[Room, DefaultEdge]) extends Entity {


  override var shape: Shape = _
  override var debug: Boolean = _

  var visionModule: VisionModule = _
  var controls: (Int, Int, Int, Int) = _

  var actor: ActorRef = _
  var atDoor: Boolean = _

  var isFree: Boolean = _
  var doorToEnter: Door = _

  var followModule: FollowModule = _
  var behaviorModule: BehaviorModule = _
  var movementModule: MovementModule = _
  var lastEntryDoor: Door = _
  var weightedGraph: SimpleWeightedGraph[Room, DefaultWeightedEdge] = _

  var avoidFireTimer: Timer = _

  var followTimer: Timer = _

  def init(): Unit = {

    shape = new Rectangle(0, 0, Globals.AGENT_SIZE, Globals.AGENT_SIZE)



    atDoor = false


    followTimer = new Timer(Configuration.AGENT_FOLLOW_TIMER)

    isFree = false

    debug = false


    lastEntryDoor = null

    avoidFireTimer = new Timer(3000)
    avoidFireTimer.time = avoidFireTimer.timeout+1

    weightedGraph = Globals.copyGraph(roomGraph)


    followModule = FollowModule()
    behaviorModule = BehaviorModule(this)
    visionModule = VisionModule(this)
    movementModule = MovementModule(this)

    while (!isFree) {
      shape.setX(Random.nextInt(room.w - Globals.AGENT_SIZE))
      shape.setY(Random.nextInt(room.h - Globals.AGENT_SIZE))

      val collisionDetails = Globals.manageCollisions(room, this, 0, 0)

      if (!collisionDetails.colX && !collisionDetails.colY) {
        isFree = true
      }
    }



  }

  def setControls(controls: (Int, Int, Int, Int)): Unit = {
    this.controls = controls
  }

  def update(gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    visionModule.update(delta)

    movementModule.update(gc, delta, renderScale)

    //visionModule.update(delta)

    if (debug) {
      Statistics.params.put("Location", room.name + " " + shape.getCenterX + " " + shape.getCenterY)
    }
  }

  override def onCollision(entity: Entity): Unit = {
    if (entity.getClass == classOf[Agent]) {

      val agent: Agent = entity.asInstanceOf[Agent]

      movementModule.pushBack(this, agent)

    }

    //temporary solution, move evacuated outside map
    if (entity.getClass == classOf[MeetPoint]) {
      shape.setX(1000)
      shape.setY(1000)
      room.agentList -= this
    }

    if (entity.getClass == classOf[Door]) {
      val door = entity.asInstanceOf[Door]
      val leadingToDoor = door.leadingToDoor
      var normalVector = new Vector2f(movementModule.currentVelocityX, movementModule.currentVelocityY)

      normalVector.normalise()

      for (_ <- 1 to 36) {
        normalVector.setTheta(normalVector.getTheta + 10)
        val spotX = leadingToDoor.posX + normalVector.x * 50
        val spotY = leadingToDoor.posY + normalVector.y * 50

        if (!Globals.isRectOccupied(leadingToDoor.room, spotX - 5, spotY - 5, shape.getWidth + 10, shape.getHeight + 10, this)) {
          changeRoom(door, spotX, spotY)
          return
        }
      }
    }
  }



  override def changeRoom(entryDoor: Door, newX: Float, newY: Float): Unit = {
    lastEntryDoor = entryDoor

    if (doorToEnter != entryDoor) {
      if (controlScheme != ControlScheme.Manual) {
        return
      }
    }


    if (!roomGraph.containsVertex(entryDoor.leadingToDoor.room)) {
      addRoomToGraph(entryDoor.leadingToDoor.room)
    }


    atDoor = false

    followTimer.reset()

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

    currentBehavior.afterChangeRoom()
  }

  def setActor(actor: ActorRef): Unit = {
    this.actor = actor
  }

  def draw(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.drawImage(image, room.x + shape.getX - offsetX, room.y + shape.getY - offsetY)

    visionModule.draw(g, offsetX, offsetY)
  }

  def drawName(g: Graphics, offsetX: Float, offsetY: Float): Unit = {
    g.setColor(Color.pink)
    if(movementModule.beingPushed) {
      g.setColor(Color.blue)
    }
    g.drawString(name, room.x + shape.getX - 10 - offsetX, room.y + shape.getY - 40 - offsetY)
    g.setColor(currentBehavior.color)

    var tag: String = ""
    if (currentBehavior.name == FollowBehavior.name && followModule.followedAgent.name != null) {
      tag = "[" + currentBehavior.name + " " + followModule.followedAgent.name + "]"
    }
    else {
      tag = "[" + currentBehavior.name + "]"

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

    doorLeadingToRoom(weightedGraph, meetPointRoom)
  }

  def doorLeadingToRoom(graph: SimpleWeightedGraph[Room, DefaultWeightedEdge], targetRoom: Room): Door = {
    if (!graph.containsVertex(targetRoom)) {
      return null
    }

    var graphCopy: SimpleWeightedGraph[Room, DefaultWeightedEdge] = Globals.copyGraph(weightedGraph)

    var doorDistances: mutable.Map[Door, Float] = mutable.Map[Door, Float]()

    for (door <- room.doorList) {
      doorDistances.put(door, getDistanceTo(door))
    }

    val maxDistance: Float = doorDistances.values.max

    for (pair <- doorDistances) {
      val edge: DefaultWeightedEdge = graphCopy.getEdge(room, pair._1.leadingToDoor.room)
      if (edge != null) graphCopy.setEdgeWeight(edge, pair._2 / maxDistance)
    }

    val path = shortestPath(graphCopy, room, targetRoom)

    if (path == null) return null

    for (door: Door <- room.doorList) {
      // return NEXT room on path (second element)
      if (path.size() > 1 && door.leadingToDoor.room == path.get(1)) {
        return door
      }
    }


    null

  }


  def shortestPath(graph: SimpleWeightedGraph[Room, DefaultWeightedEdge], from: Room, to: Room): java.util.List[Room] = {
    import org.jgrapht.alg.shortestpath.DijkstraShortestPath
    val dijkstraShortestPath = new DijkstraShortestPath(graph)

    var shortestPath: java.util.List[Room] = null
    try {
      shortestPath = dijkstraShortestPath.getPath(room, to).getVertexList

    }
    catch {
      case _: NullPointerException =>
        return null
      case _: IllegalArgumentException =>
        return null
    }

    shortestPath
  }

  def currentBehavior: Behavior = behaviorModule.currentBehavior
  def setBehavior(behaviorName: String): Unit = behaviorModule.setBehavior(behaviorName)
  def follow(agent: Agent, posX: Float, posY: Float, atDistance: Float): Unit = currentBehavior.follow(agent, posX, posY, atDistance)
}
