package lbtweb.controllers

import lbtweb._
import net.liftweb.json._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.collection.immutable.Seq
import scala.io.Source

class Controllers(config: ServerConfig) {

  implicit def iterebleWithAvg[T:Numeric](data:Iterable[T]) = new {
    def avg = Commons.average(data)
  }

  val dtf: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss")
  implicit val formats = DefaultFormats
  val serverPrefix = config.url + ":" + config.port + "/historical/"

  def getRouteList: List[(String, List[(String, String)])] = {
    val res = Source.fromURL(serverPrefix + "routelist").mkString
    val split = parse(res).extract[List[BusRouteWithTowards]]
      .groupBy(busRoute => busRoute.id)
      .map(group => (group._1, group._2
        .map(route => (route.direction, route.towards))
        .sortBy(x => x._1)))
      .toList
      .partition(routeList => routeList._1.forall(Character.isDigit))
    split._1.sortBy(_._1.toInt) ++ split._2.sortBy(letterRoute => (letterRoute._1.charAt(0), letterRoute._1.substring(1)))
  }

  def getStopList(busRoute: BusRoute): List[BusStop] = {
    val res = Source.fromURL(serverPrefix + "stoplist/" + busRoute.id + "/" + busRoute.direction).mkString
    parse(res).extract[List[BusStop]]
  }

  def getRouteArrivalHistoryData(busRoute: BusRoute): List[(String, Map[Int, String])] = {
    val res = Source.fromURL(serverPrefix + "busroute/" + busRoute.id + "/" + busRoute.direction).mkString
    val received = parse(res).extract[List[IncomingHistoricalRecord]]
    assert(!received.exists(x => x.busRoute != busRoute))
    received.sortBy(x => x.stopRecords.head.arrivalTime)(Ordering[Long].reverse)
      .map(x => (x.vehicleID, x.stopRecords.sortBy(_.seqNo)
        .map(x => x.seqNo -> dtf.print(x.arrivalTime)).toMap))
  }

  def getStopArrivalHistory(busStopID: String): List[(BusRoute, String, String)] = {
    val res = Source.fromURL(serverPrefix + "stop/" + busStopID).mkString
    val received = parse(res).extract[List[IncomingHistoricalRecord]]
    received
      .map(x => (x.busRoute, x.vehicleID, x.stopRecords.find(y => y.stopID == busStopID).get.arrivalTime))
      .sortBy(x => x._3)(Ordering[Long].reverse)
      .map(x => (x._1, x._2, dtf.print(x._3)))
  }

  def getVehicleArrivalHistory(vehicleID: String): List[(BusRoute, List[(Int, String, String, String)])] = {
    val res = Source.fromURL(serverPrefix + "vehicle/" + vehicleID).mkString
    val received = parse(res).extract[List[IncomingHistoricalRecord]]
    assert(!received.exists(x => x.vehicleID != vehicleID))
    received.map(x => (x.busRoute, x.stopRecords.sortBy(_.arrivalTime)(Ordering[Long].reverse)
      .map(y => (y.seqNo, y.stopID, y.stopName, dtf.print(y.arrivalTime)))))
  }

  def getRouteArrivalHistoryStats(busRoute: BusRoute): Map[Int, RouteStats] = {
    val res = Source.fromURL(serverPrefix + "busroute/" + busRoute.id + "/" + busRoute.direction).mkString
    val received = parse(res).extract[List[IncomingHistoricalRecord]]
    assert(!received.exists(x => x.busRoute != busRoute))
    val timeDiffBetweenStops  = for {
      seqNoArrTime <- received.map(x => x.stopRecords.map(y => (y.seqNo, y.arrivalTime)))
      seqNoArrTimeWithoutLast = seqNoArrTime.dropRight(1)
      result = seqNoArrTimeWithoutLast.dropRight(1).map(_._1) zip (seqNoArrTimeWithoutLast drop 1, seqNoArrTimeWithoutLast).zipped.map(_._2 - _._2)
    } yield result
    timeDiffBetweenStops.flatten.groupBy(_._1).mapValues(x => RouteStats((x.map(_._2).avg / 1000).round))
  }

//  def getStopDetails(stopID: String) = {
//    ???
//  }
}
