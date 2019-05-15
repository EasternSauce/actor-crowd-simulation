package com.kamilkurp.util

import com.kamilkurp.util.ControlScheme.Value

object Screen extends Enumeration {
  type Screen = Value
  val MainMenu, Simulation = Value

  var currentScreen: Screen = MainMenu

}
