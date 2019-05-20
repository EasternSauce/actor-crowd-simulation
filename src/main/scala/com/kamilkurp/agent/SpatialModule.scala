package com.kamilkurp.agent

import java.util

import com.kamilkurp.behavior.{FollowBehavior, LeaderBehavior}
import com.kamilkurp.building.{Door, Room}
import com.kamilkurp.util.Globals
import org.jgrapht.graph.{DefaultDirectedWeightedGraph, DefaultWeightedEdge}
import org.newdawn.slick.Color
import org.newdawn.slick.geom.{Line, Shape, Transform}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.compat.Platform.ConcurrentModificationException
import scala.util.Random

class SpatialModule private() {


  var mentalMapGraph: DefaultDirectedWeightedGraph[Room, DefaultWeightedEdge] = _
  var knownFireLocations: mutable.Set[Room] = _
  private var agent: Agent = _

  def addRoomToGraph(room: Room): Unit = {
    mentalMapGraph.addVertex(room)

    for (door <- room.doorList) {
      if (mentalMapGraph.containsVertex(door.leadingToDoor.currentRoom)) {
        val edge1: DefaultWeightedEdge = mentalMapGraph.addEdge(room, door.leadingToDoor.currentRoom)
        mentalMapGraph.setEdgeWeight(edge1, 1.0f)
        val edge2: DefaultWeightedEdge = mentalMapGraph.addEdge(door.leadingToDoor.currentRoom, room)
        mentalMapGraph.setEdgeWeight(edge2, 1.0f)
      }
    }
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

        removeVertex(location)

        agent.intendedDoor = findDoorToEnterNext()
      }
    })
  }

  def removeVertex(vertex: Room): Unit = {
    mentalMapGraph.removeVertex(vertex)

  }

  def findDoorToEnterNext(): Door = {
    var doorDistances: mutable.Map[Door, Float] = mutable.Map[Door, Float]()

    for (door <- agent.currentRoom.doorList) {
      doorDistances.put(door, agent.getDistanceTo(door))
    }

    val maxDistance: Float = doorDistances.values.max


    var meetPointRoomList: ListBuffer[Room] = new ListBuffer[Room]()
    var pathLengthList: ListBuffer[Float] = new ListBuffer[Float]()

    val vertices: Array[AnyRef] = agent.buildingPlanGraph.vertexSet().toArray

    //for each meetpoint room
    for (ref <- vertices) {
      val room = ref.asInstanceOf[Room]
      if (room.meetPointList.nonEmpty) {

        meetPointRoomList += room
        val path: util.List[Room] = shortestPath(mentalMapGraph, agent.currentRoom, room)

        var doorDistanceFactor = 0f

        for (door <- agent.currentRoom.doorList) {
          if (path != null && path.size() > 1 && door.leadingToDoor.currentRoom == path.get(1)) {
            doorDistanceFactor = agent.getDistanceTo(door)/maxDistance
          }
        }

        if (path != null) {
          val length: Int = path.size()
          pathLengthList += length + doorDistanceFactor

        }
        else {
          pathLengthList += Int.MaxValue
        }

      }
    }

    val index = pathLengthList.indexOf(pathLengthList.min)

    val result = doorLeadingToRoom(mentalMapGraph, meetPointRoomList(index))

    result
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
        graphCopy.setEdgeWeight(edge, pair._2 / maxDistance)
      }
    }

    val path = shortestPath(graphCopy, agent.currentRoom, targetRoom)


    if (path == null) {
      return null
    }

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
      while(shortestPath == null) {
        try{
          shortestPath = dijkstraShortestPath.getPath(agent.currentRoom, to).getVertexList
        } catch {
          case _: ConcurrentModificationException =>
            shortestPath = null
        }
      }


    }
    catch {
      case _: NullPointerException =>
        return null
      case _: IllegalArgumentException =>
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