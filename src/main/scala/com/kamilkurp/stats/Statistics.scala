package com.kamilkurp.stats

import scala.collection.mutable

object Statistics {

  var params: mutable.LinkedHashMap[String, String] = _

  params = mutable.LinkedHashMap[String, String]()


  params.put("Total agents", "0")
  params.put("Total evacuated", "0")
  params.put("Time", "0")


  params.put("padding1", "hide")
  params.put("padding2", "hide")
  params.put("padding3", "hide")
  params.put("padding4", "hide")
  params.put("padding5", "hide")
  params.put("Agent name", "")
  params.put("Start room", "")
  params.put("Velocity x", "")
  params.put("Velocity y", "")
  params.put("Behavior", "")
  params.put("Stress level", "")
  params.put("Stress resistance", "")






}