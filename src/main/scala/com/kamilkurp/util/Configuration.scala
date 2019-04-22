package com.kamilkurp.util

object Configuration {

  var AGENT_IMAGE_LOCATION: String = "character.png"
  var DOOR_IMAGE_LOCATION: String = "door.png"
  var FLAMES_IMAGE_LOCATION: String = "fire.png"
  var BUILDING_PLAN_LOCATION: String = "building.txt"

  var UNTIL_ALARM_TIME: Int = 5000

  var NUMBER_OF_AGENTS: Int = 100
  val ADD_MANUAL_AGENT: Boolean = false
  val MANUAL_AGENT_NAME: String = "Player"
  val FLAME_PROPAGATION_SPEED: Float = 5

  val AGENT_SPEED: Float = 0.2f
  val LEADER_PERCENTAGE: Float = 0.5f
  val AGENT_TURN_SPEED: Int = 12

  val AGENT_SLOW_TIMER: Int = 3000
  val AGENT_LOOK_TIMER: Int = 50
  val AGENT_MOVE_OUT_OF_WAY_TIMER: Int = 1000
  val AGENT_BROADCAST_TIMER: Int = 300
  val WAIT_AT_DOOR_TIMER: Int = 300
  val AGENT_VISION_TIMER: Int = 500
  val AGENT_IDLE_TIMER: Int = 500
  val AGENT_FOLLOW_TIMER: Int = 5000

}
