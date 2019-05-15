package com.kamilkurp.agent

import com.kamilkurp.behavior._
import com.kamilkurp.util.Configuration

import scala.collection.mutable
import scala.util.Random

class BehaviorModule private() {
  private var behaviorMap: mutable.HashMap[String, Behavior] = _
  private var _currentBehavior: String = _

  private var agent: Agent = _


  def setBehavior(behaviorName: String): Unit = {
    _currentBehavior = behaviorName
    behaviorMap(behaviorName).init()
  }

  private def getBehavior(behaviorName: String): Behavior = behaviorMap(behaviorName)

  def currentBehavior: Behavior = behaviorMap(_currentBehavior)

}


object BehaviorModule {
  def apply(agent: Agent): BehaviorModule = {
    val behaviorModule = new BehaviorModule()

    behaviorModule.agent = agent

    behaviorModule.behaviorMap = mutable.HashMap.empty[String, Behavior]

    behaviorModule.behaviorMap += (FollowBehavior.name -> new FollowBehavior(agent, FollowBehavior.name, FollowBehavior.color))
    behaviorModule.behaviorMap += (IdleBehavior.name -> new IdleBehavior(agent, IdleBehavior.name, IdleBehavior.color))
    behaviorModule.behaviorMap += (LeaderBehavior.name -> new LeaderBehavior(agent, LeaderBehavior.name, LeaderBehavior.color))
    behaviorModule.behaviorMap += (HoldMeetPointBehavior.name -> new HoldMeetPointBehavior(agent, HoldMeetPointBehavior.name, HoldMeetPointBehavior.color))
    behaviorModule.behaviorMap += (SearchExitBehavior.name -> new SearchExitBehavior(agent, SearchExitBehavior.name, SearchExitBehavior.color))
    behaviorModule.behaviorMap += (AvoidFireBehavior.name -> new AvoidFireBehavior(agent, AvoidFireBehavior.name, AvoidFireBehavior.color))
    behaviorModule.behaviorMap += (StationaryBehavior.name -> new StationaryBehavior(agent, StationaryBehavior.name, StationaryBehavior.color))

    var startBehavior = IdleBehavior.name

    if (Random.nextFloat() < Configuration.leaderPercentage) {
      startBehavior = LeaderBehavior.name
    }

    if (agent.name == Configuration.MANUAL_AGENT_NAME) {
      startBehavior = LeaderBehavior.name
    }

    behaviorModule.setBehavior(startBehavior)

    behaviorModule
  }


}