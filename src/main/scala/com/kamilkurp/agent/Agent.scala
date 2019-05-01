package com.kamilkurp.agent

import akka.actor.ActorRef
import com.kamilkurp.behavior._
import com.kamilkurp.building.{Door, MeetPoint, Room}
import com.kamilkurp.entity.Entity
import com.kamilkurp.util.ControlScheme.ControlScheme
import com.kamilkurp.util.{Configuration, ControlScheme, Globals, Timer}
import org.jgrapht.graph.{DefaultWeightedEdge, SimpleWeightedGraph}
import org.newdawn.slick.geom.{Shape, _}
import org.newdawn.slick.{Color, GameContainer, Graphics, Image}

import scala.collection.mutable
import scala.util.Random

class Agent private (var name: String, var room: Room, val controlScheme: ControlScheme, var image: Image, var buildingPlanGraph: SimpleWeightedGraph[Room, DefaultWeightedEdge]) extends Entity {


  override var shape: Shape = _
  override var debug: Boolean = _

  var visionModule: VisionModule = _
  var controls: (Int, Int, Int, Int) = _

  var actor: ActorRef = _

  var isFree: Boolean = _
  var doorToEnter: Door = _

  var behaviorModule: BehaviorModule = _
  var movementModule: MovementModule = _
  var lastEntryDoor: Door = _
  var mentalMapGraph: SimpleWeightedGraph[Room, DefaultWeightedEdge] = _

  var avoidFireTimer: Timer = _

  var followTimer: Timer = _

  var followX: Float = _
  var followY: Float = _
  var followDistance: Float = _
  var followedAgent: Agent = _

  var knownFireLocations: mutable.Set[Room] = _

  def setControls(controls: (Int, Int, Int, Int)): Unit = {
    this.controls = controls
  }

  def update(gc: GameContainer, delta: Int, renderScale: Float): Unit = {
    visionModule.update(delta)

    movementModule.update(gc, delta, renderScale)

  }

  override def onCollision(entity: Entity): Unit = {
    if (entity.getClass == classOf[Agent]) {

      val that: Agent = entity.asInstanceOf[Agent]

      if (that.currentBehavior.name == FollowBehavior.name || that.currentBehavior.name == SearchExitBehavior.name) {
        movementModule.pushBack(this, that)
      }

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


    if (!mentalMapGraph.containsVertex(entryDoor.leadingToDoor.room)) {
      addRoomToGraph(entryDoor.leadingToDoor.room)
    }

    followTimer.reset()

    val newRoom: Room = entryDoor.leadingToDoor.room

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
    if (currentBehavior.name == FollowBehavior.name && followedAgent.name != null) {
      tag = "[" + currentBehavior.name + " " + followedAgent.name + "]"
    }
    else {
      tag = "[" + currentBehavior.name + "]"

    }
    g.drawString(tag, room.x + shape.getX - 10 - offsetX, room.y + shape.getY - 25 - offsetY)

  }

  def addRoomToGraph(room: Room): Unit = {
    mentalMapGraph.addVertex(room)

    for (door <- room.doorList) {
      if (mentalMapGraph.containsVertex(door.leadingToDoor.room)) {
        val edge: DefaultWeightedEdge = mentalMapGraph.addEdge(room, door.leadingToDoor.room)
        mentalMapGraph.setEdgeWeight(edge, 1.0f)
      }
    }
  }


  def findDoorToEnterNext(): Door = {

    var meetPointRoom: Room = null

    val vertices: Array[AnyRef] = buildingPlanGraph.vertexSet().toArray
    for (ref <- vertices) {
      val room = ref.asInstanceOf[Room]
      if (room.meetPointList.nonEmpty) meetPointRoom = room
    }

    doorLeadingToRoom(mentalMapGraph, meetPointRoom)
  }

  def doorLeadingToRoom(graph: SimpleWeightedGraph[Room, DefaultWeightedEdge], targetRoom: Room): Door = {

    if (!graph.containsVertex(targetRoom)) {
      return null
    }



    var graphCopy: SimpleWeightedGraph[Room, DefaultWeightedEdge] = Globals.copyGraph(graph)

    var doorDistances: mutable.Map[Door, Float] = mutable.Map[Door, Float]()

    for (door <- room.doorList) {
      doorDistances.put(door, getDistanceTo(door))
    }

    val maxDistance: Float = doorDistances.values.max

    for (pair <- doorDistances) {
      val edge: DefaultWeightedEdge = graphCopy.getEdge(room, pair._1.leadingToDoor.room)
      if (edge != null) {
        if (knownFireLocations.contains(pair._1.leadingToDoor.room)) {
          graphCopy.setEdgeWeight(edge, 100f)
        }
        else {
          graphCopy.setEdgeWeight(edge, pair._2 / maxDistance)
        }

      }
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

    if (currentBehavior.name == FollowBehavior.name) {
    }

    var shortestPath: java.util.List[Room] = null
    try {
      shortestPath = dijkstraShortestPath.getPath(room, to).getVertexList

    }
    catch {
      case _: NullPointerException =>
        if (currentBehavior.name == FollowBehavior.name) {
        }
        return null
      case _: IllegalArgumentException =>
        if (currentBehavior.name == FollowBehavior.name) {
        }
        return null
    }

    shortestPath
  }

  def currentBehavior: Behavior = behaviorModule.currentBehavior
  def setBehavior(behaviorName: String): Unit = behaviorModule.setBehavior(behaviorName)

  def followLeader(leader: Agent): Unit = {
    followedAgent = leader
    followX = leader.shape.getCenterX
    followY = leader.shape.getCenterY
    followDistance = 120
    setBehavior(FollowBehavior.name)
  }

  def broadcast(msg: AgentMessage, timer: Timer): Unit = {
    if (timer.timedOut()) {
      room.agentList.foreach(that => {
        if (getDistanceTo(that) < 400 && that != this) {
          that.actor ! msg
        }
      })
      timer.reset()
    }
  }
}


object Agent {
  def apply(name: String, room: Room, controlScheme: ControlScheme, image: Image, buildingPlanGraph: SimpleWeightedGraph[Room, DefaultWeightedEdge]): Agent = {
    val agent = new Agent(name, room, controlScheme, image, buildingPlanGraph)

    agent.shape = new Rectangle(0, 0, Globals.AGENT_SIZE, Globals.AGENT_SIZE)

    agent.followTimer = new Timer(Configuration.AGENT_FOLLOW_TIMER)

    agent.isFree = false

    agent.debug = false


    agent.lastEntryDoor = null

    agent.avoidFireTimer = new Timer(3000)
    agent.avoidFireTimer.time = agent.avoidFireTimer.timeout+1

    agent.behaviorModule = BehaviorModule(agent)
    agent.visionModule = VisionModule(agent)
    agent.movementModule = MovementModule(agent)

    if (agent.currentBehavior.name == LeaderBehavior.name) {
      agent.mentalMapGraph = Globals.copyGraph(buildingPlanGraph)
    }
    else {
      agent.mentalMapGraph = new SimpleWeightedGraph[Room, DefaultWeightedEdge](classOf[DefaultWeightedEdge])
      agent.addRoomToGraph(room)

    }

    while (!agent.isFree) {
      agent.shape.setX(Random.nextInt(room.w - Globals.AGENT_SIZE))
      agent.shape.setY(Random.nextInt(room.h - Globals.AGENT_SIZE))

      val collisionDetails = Globals.manageCollisions(room, agent, 0, 0)

      if (!collisionDetails.colX && !collisionDetails.colY) {
        agent.isFree = true
      }
    }

    agent.followX = 0
    agent.followY = 0
    agent.followDistance = 0

    agent.followedAgent = null

    agent.followTimer = new Timer(Configuration.AGENT_FOLLOW_TIMER)

    agent.knownFireLocations = mutable.Set()

    agent
  }
}