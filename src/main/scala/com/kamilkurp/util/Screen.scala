package com.kamilkurp.util

object Screen extends Enumeration {
  type Screen = Value
  val MainMenu, Simulation = Value

  var currentScreen: Screen = MainMenu

}
