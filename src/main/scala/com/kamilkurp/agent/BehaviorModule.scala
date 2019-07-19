package com.kamilkurp.agent

import com.kamilkurp.behavior._
import com.kamilkurp.util.Configuration

import scala.collection.mutable
import scala.util.Random

class BehaviorModule private() {
  private var behaviorMap: mutable.HashMap[String, Behavior] = _
  private var _currentBehavior: String = _

  var previousBehavior: String = _

  private var agent: Agent = _
  var startBehavior: String = _


  def setBehavior(behaviorName: String): Unit = {
    _currentBehavior = behaviorName
  }

  def initBehavior(behaviorName: String): Unit = {
    behaviorMap(behaviorName).init()
  }

  private def getBehavior(behaviorName: String): Behavior = behaviorMap(behaviorName)

  def currentBehavior: Behavior = behaviorMap(_currentBehavior)

}


object BehaviorModule {
  def apply(agent: Agent): BehaviorModule = {
    val behaviorModule = new BehaviorModule()

    behaviorModule.agent = agent

    behaviorModule.startBehavior = IdleBehavior.name

    behaviorModule.previousBehavior = null

    behaviorModule.behaviorMap = mutable.HashMap.empty[String, Behavior]

    behaviorModule.behaviorMap += (FollowBehavior.name -> new FollowBehavior(agent, FollowBehavior.name, FollowBehavior.color))
    behaviorModule.behaviorMap += (IdleBehavior.name -> new IdleBehavior(agent, IdleBehavior.name, IdleBehavior.color))
    behaviorModule.behaviorMap += (LeaderBehavior.name -> new LeaderBehavior(agent, LeaderBehavior.name, LeaderBehavior.color))
    behaviorModule.behaviorMap += (HoldMeetPointBehavior.name -> new HoldMeetPointBehavior(agent, HoldMeetPointBehavior.name, HoldMeetPointBehavior.color))
    behaviorModule.behaviorMap += (SearchExitBehavior.name -> new SearchExitBehavior(agent, SearchExitBehavior.name, SearchExitBehavior.color))
    behaviorModule.behaviorMap += (AvoidFireBehavior.name -> new AvoidFireBehavior(agent, AvoidFireBehavior.name, AvoidFireBehavior.color))
    behaviorModule.behaviorMap += (StationaryBehavior.name -> new StationaryBehavior(agent, StationaryBehavior.name, StationaryBehavior.color))
    behaviorModule.behaviorMap += (PanicBehavior.name -> new PanicBehavior(agent, PanicBehavior.name, PanicBehavior.color))
    behaviorModule.behaviorMap += (PickupBelongingsBehavior.name -> new PickupBelongingsBehavior(agent, PickupBelongingsBehavior.name, PickupBelongingsBehavior.color))
    behaviorModule.behaviorMap += (IgnoreAlarmBehavior.name -> new IgnoreAlarmBehavior(agent, IgnoreAlarmBehavior.name, IgnoreAlarmBehavior.color))
    behaviorModule.behaviorMap += (HelpBehavior.name -> new HelpBehavior(agent, HelpBehavior.name, HelpBehavior.color))


    if (Random.nextFloat() < Configuration.leaderPercentage) {
      if (Random.nextFloat() < Configuration.PICKUP_BELONGINGS_PERCENTAGE) {
        behaviorModule.startBehavior = PickupBelongingsBehavior.name
      }
      else {
        behaviorModule.startBehavior = LeaderBehavior.name
      }
    }

    if (agent.name == Configuration.MANUAL_AGENT_NAME) {
      behaviorModule.startBehavior = LeaderBehavior.name
    }


    behaviorModule
  }


}