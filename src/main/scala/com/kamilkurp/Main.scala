package com.kamilkurp

import scala.language.postfixOps

object Main extends App {

  import org.newdawn.slick.AppGameContainer

  var gameContainer = new AppGameContainer(new Simulation("Simple Slick Game"))
  gameContainer.setDisplayMode(Globals.WINDOW_X, Globals.WINDOW_Y, false)
  gameContainer.start()
}