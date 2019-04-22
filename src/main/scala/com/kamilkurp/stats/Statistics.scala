package com.kamilkurp.stats

import scala.collection.mutable

object Statistics {

  var params: mutable.TreeMap[String, String] = _

  params = mutable.TreeMap[String, String]()


  params.put("Total agents", "0")
  params.put("Total evacuated", "0")



}
