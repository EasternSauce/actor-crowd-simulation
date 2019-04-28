package com.kamilkurp.agent

import com.kamilkurp.util.{Configuration, Timer}

import scala.util.Random

class FollowModule private() {

  private var _followX: Float = _
  private var _followY: Float = _
  var followDistance: Float = _
  var followedAgent: Agent = _
  var followTimer: Timer = _
  var lastSeenFollowedEntityTimer: Timer = _
  var lostSightOfFollowedEntity: Boolean = _

  def setFollow(x: Float, y: Float): Unit = {
    _followX = x
    _followY = y
  }

  def followX: Float = _followX
  def followY: Float = _followY
}

object FollowModule {
  def apply(): FollowModule = {
    val followModule = new FollowModule()

    followModule._followX = 0
    followModule._followY = 0
    followModule.followDistance = 0

    followModule.followedAgent = null


    followModule.lastSeenFollowedEntityTimer = new Timer(1000 + new Random().nextInt(600))

    followModule.lostSightOfFollowedEntity = false

    followModule.followTimer = new Timer(Configuration.AGENT_FOLLOW_TIMER)


    followModule
  }
}
