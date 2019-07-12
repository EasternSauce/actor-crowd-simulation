package com.kamilkurp.util

object Configuration {


  var AGENT_IMAGE_LOCATION: String = "character.png"
  var DOOR_IMAGE_LOCATION: String = "door.png"
  var FLAMES_IMAGE_LOCATION: String = "fire.png"
  var STAIRS_IMAGE_LOCATION: String = "stairs.png"
  var BUILDING_PLAN_LOCATION: String = "building.txt"

  var UNTIL_ALARM_TIME: Int = 5000

  var NUMBER_OF_AGENTS: Int = 250
  val ADD_MANUAL_AGENT: Boolean = false
  val MANUAL_AGENT_NAME: String = "Player"
  val FLAME_PROPAGATION_SPEED: Float = 5

  val AGENT_SPEED: Float = 0.2f
  val AGENT_ACCELERATION: Float = 0.005f
  val LEADER_PERCENTAGE: Float = 0.3f
  var PICKUP_BELONGINGS_PERCENTAGE: Float = 0.3f
  var IGNORE_ALARM_PERCENTAGE: Float = 0.3f
  var RUNNER_PERCENTAGE: Float = 0.5f
  val AGENT_TURN_SPEED: Int = 12
  var CHANCE_TO_TRIP: Float = 0.1f

  val AGENT_SLOW_TIMER: Int = 3000
  val AGENT_LOOK_TIMER: Int = 50
  val AGENT_MOVE_OUT_OF_WAY_TIMER: Int = 1000
  val AGENT_BROADCAST_TIMER: Int = 300
  val WAIT_AT_DOOR_TIMER: Int = 300
  val AGENT_VISION_TIMER: Int = 500
  val AGENT_IDLE_TIMER: Int = 500
  val AGENT_FOLLOW_TIMER: Int = 5000

  val CAMERA_SPEED: Float = 3f

  val ZOOM_SPEED: Float = 0.025f

  var AGENT_BROADCAST_DISTANCE = 400

  var untilAlarmTime: Int = UNTIL_ALARM_TIME
  var buildingPlanLocation: String = BUILDING_PLAN_LOCATION
  var numberOfAgents: Int = NUMBER_OF_AGENTS
  var agentVisionTimer: Int = AGENT_VISION_TIMER
  var agentTurnSpeed: Int = AGENT_TURN_SPEED
  var agentBroadcastTimer: Int = AGENT_BROADCAST_TIMER
  var leaderPercentage: Float = LEADER_PERCENTAGE
  var flamePropagationSpeed: Float = FLAME_PROPAGATION_SPEED
  var agentBroadcastDistance: Int = AGENT_BROADCAST_DISTANCE

//  var argument: String = ""
}
