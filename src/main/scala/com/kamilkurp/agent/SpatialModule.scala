package com.kamilkurp.agent

import com.kamilkurp.behavior.{FollowBehavior, LeaderBehavior}
import com.kamilkurp.building.{Door, Room}
import com.kamilkurp.util.Globals
import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class SpatialModule private() {


  var mentalMapGraph: DefaultDirectedWeightedGraph[Room, DefaultWeightedEdge] = _
  var knownFireLocations: mutable.Set[Room] = _
  private var agent: Agent = _

  def addRoomToGraph(room: Room): Unit = {
    mentalMapGraph.addVertex(room)

    for (door <- room.doorList) {
      if (mentalMapGraph.containsVertex(door.leadingToDoor.currentRoom)) {
        val edge: DefaultWeightedEdge = mentalMapGraph.addEdge(room, door.leadingToDoor.currentRoom)
        mentalMapGraph.setEdgeWeight(edge, 1.0f)
      }
    }
  }

  def onSpottingFire(): Unit = {
    if (agent.doorToEnter != null) removeEdge(agent.currentRoom, agent.doorToEnter.leadingToDoor.currentRoom)

    knownFireLocations += agent.currentRoom

    if (agent.currentBehavior.name == LeaderBehavior.name) agent.doorToEnter = findDoorToEnterNext()
  }

  def removeEdge(from: Room, to: Room): Unit = {
    val edgeArray = mentalMapGraph.edgeSet().toArray

    var toRemove: ListBuffer[DefaultWeightedEdge] = new ListBuffer[DefaultWeightedEdge]()
    for (edgeRef <- edgeArray) {
      val edge = edgeRef.asInstanceOf[DefaultWeightedEdge]

      if (mentalMapGraph.getEdgeSource(edge) == from && mentalMapGraph.getEdgeTarget(edge) == to) {
        toRemove += edge
      }

    }

    for (edge <- toRemove) {
      mentalMapGraph.removeEdge(edge)

    }

  }

  def onReceiveFireLocationInfo(fireLocations: mutable.Set[Room]): Unit = {
    fireLocations.foreach(location => {
      if (!knownFireLocations.contains(location)) {
        knownFireLocations += location


        agent.doorToEnter = findDoorToEnterNext()
      }
    })
  }

  def findDoorToEnterNext(): Door = {

    var meetPointRoom: Room = null

    val vertices: Array[AnyRef] = agent.buildingPlanGraph.vertexSet().toArray
    for (ref <- vertices) {
      val room = ref.asInstanceOf[Room]
      if (room.meetPointList.nonEmpty) meetPointRoom = room
    }

    doorLeadingToRoom(mentalMapGraph, meetPointRoom)
  }

  def doorLeadingToRoom(graph: DefaultDirectedWeightedGraph[Room, DefaultWeightedEdge], targetRoom: Room): Door = {

    if (!graph.containsVertex(targetRoom)) {
      return null
    }


    var graphCopy: DefaultDirectedWeightedGraph[Room, DefaultWeightedEdge] = Globals.copyGraph(graph)

    var doorDistances: mutable.Map[Door, Float] = mutable.Map[Door, Float]()

    for (door <- agent.currentRoom.doorList) {
      doorDistances.put(door, agent.getDistanceTo(door))
    }

    val maxDistance: Float = doorDistances.values.max

    for (pair <- doorDistances) {
      val edge: DefaultWeightedEdge = graphCopy.getEdge(agent.currentRoom, pair._1.leadingToDoor.currentRoom)
      if (edge != null) {
        if (knownFireLocations.contains(pair._1.leadingToDoor.currentRoom)) {
          graphCopy.setEdgeWeight(edge, 100f)
        }
        else {
          graphCopy.setEdgeWeight(edge, pair._2 / maxDistance)
        }

      }
    }

    val path = shortestPath(graphCopy, agent.currentRoom, targetRoom)


    if (path == null) return null

    for (door: Door <- agent.currentRoom.doorList) {
      // return NEXT room on path (second element)
      if (path.size() > 1 && door.leadingToDoor.currentRoom == path.get(1)) {
        return door
      }
    }


    null

  }

  def shortestPath(graph: DefaultDirectedWeightedGraph[Room, DefaultWeightedEdge], from: Room, to: Room): java.util.List[Room] = {
    import org.jgrapht.alg.shortestpath.DijkstraShortestPath
    val dijkstraShortestPath = new DijkstraShortestPath(graph)

    if (agent.currentBehavior.name == FollowBehavior.name) {
    }

    var shortestPath: java.util.List[Room] = null
    try {
      shortestPath = dijkstraShortestPath.getPath(agent.currentRoom, to).getVertexList

    }
    catch {
      case _: NullPointerException =>
        if (agent.currentBehavior.name == FollowBehavior.name) {
        }
        return null
      case _: IllegalArgumentException =>
        if (agent.currentBehavior.name == FollowBehavior.name) {
        }
        return null
    }

    shortestPath
  }
}

object SpatialModule {
  def apply(agent: Agent): SpatialModule = {
    val spatialModule: SpatialModule = new SpatialModule()

    spatialModule.agent = agent

    spatialModule.knownFireLocations = mutable.Set()

    if (agent.currentBehavior.name == LeaderBehavior.name) {
      spatialModule.mentalMapGraph = Globals.copyGraph(agent.buildingPlanGraph)
    }
    else {
      spatialModule.mentalMapGraph = new DefaultDirectedWeightedGraph[Room, DefaultWeightedEdge](classOf[DefaultWeightedEdge])
      spatialModule.addRoomToGraph(agent.currentRoom)

    }

    spatialModule
  }
}