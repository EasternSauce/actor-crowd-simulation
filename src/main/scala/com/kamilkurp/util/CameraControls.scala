package com.kamilkurp.util

import com.kamilkurp.simulation.CameraView
import org.newdawn.slick.{GameContainer, Input}


class CameraControls {

  var renderScale: Float = 1.0f


  def handleControls(gc: GameContainer, i: Int): Unit = {
    if (gc.getInput.isKeyDown(Input.KEY_DOWN)) {
      CameraView.y = CameraView.y + (Configuration.CAMERA_SPEED * i.toFloat)
    }
    if (gc.getInput.isKeyDown(Input.KEY_UP)) {
      CameraView.y = CameraView.y - (Configuration.CAMERA_SPEED * i.toFloat)
    }
    if (gc.getInput.isKeyDown(Input.KEY_RIGHT)) {
      CameraView.x = CameraView.x + (Configuration.CAMERA_SPEED * i.toFloat)
    }
    if (gc.getInput.isKeyDown(Input.KEY_LEFT)) {
      CameraView.x = CameraView.x - (Configuration.CAMERA_SPEED * i.toFloat)
    }

    if (gc.getInput.isKeyDown(Input.KEY_SUBTRACT)) {


      val centerX = CameraView.x + Globals.WINDOW_X * 1 / renderScale / 2
      val centerY = CameraView.y + Globals.WINDOW_Y * 1 / renderScale / 2

      renderScale /= 1 + Configuration.ZOOM_SPEED


      CameraView.x = centerX - (Globals.WINDOW_X * 1 / renderScale / 2)
      CameraView.y = centerY - (Globals.WINDOW_Y * 1 / renderScale / 2)



    }
    if (gc.getInput.isKeyDown(Input.KEY_ADD)) {



      val centerX = CameraView.x + Globals.WINDOW_X * 1 / renderScale / 2
      val centerY = CameraView.y + Globals.WINDOW_Y * 1 / renderScale / 2

      renderScale *= 1 + Configuration.ZOOM_SPEED


      CameraView.x = centerX - (Globals.WINDOW_X * 1 / renderScale / 2)
      CameraView.y = centerY - (Globals.WINDOW_Y * 1 / renderScale / 2)

    }
  }

}