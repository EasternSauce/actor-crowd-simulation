package com.kamilkurp.stats

import scala.collection.mutable

class Statistics {

  var params: mutable.Map[String, String] = _

  params = mutable.Map[String, String]()


  params.put("Total agents", "0")
  params.put("Total evacuated", "0")

}
