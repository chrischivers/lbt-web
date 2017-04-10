package lbtweb.controllers

import com.typesafe.scalalogging.StrictLogging
import lbtweb._
import net.liftweb.json._
import org.joda.time.{DateTime, Duration}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.collection.immutable.Seq
import scala.io.Source

class Controllers(config: ServerConfig) extends StrictLogging {

  implicit def iterableWithAvg[T: Numeric](data: Iterable[T]) = new {
    def avg = Commons.average(data)
  }

  implicit val formats = DefaultFormats
  val serverPrefix = config.url + ":" + config.port + "/historical/"

  def getRoutes: List[(String, List[(String, String)])] = {
    val res = Source.fromURL(serverPrefix + "routelist").mkString
    val split = parse(res).extract[List[BusRouteWithTowards]]
      .groupBy(busRoute => busRoute.name)
      .map(group => (group._1, group._2
        .map(route => (route.direction, route.towards))
        .sortBy(x => x._1)))
      .toList
      .partition(routeList => routeList._1.forall(Character.isDigit))
    split._1.sortBy(_._1.toInt) ++ split._2.sortBy(letterRoute => (letterRoute._1.charAt(0), letterRoute._1.substring(1)))
  }

  def getStopList(busRoute: BusRoute): List[BusStop] = {
    val res = Source.fromURL(serverPrefix + "stoplist/" + busRoute.name + "/" + busRoute.direction).mkString
    println(res)
    parse(res).extract[List[BusStop]]
  }

  def getRouteArrivalHistoryData(busRoute: BusRoute): List[(VehicleReg, lbtweb.Source, Map[SequenceNo, ArrivalTime])] = {
    val res = Source.fromURL(serverPrefix + "busroute/" + busRoute.name + "/" + busRoute.direction).mkString
    val received = parse(res).extract[List[IncomingHistoricalJourneyRecord]]
    assert(!received.exists(x => x.journey.busRoute != busRoute))
    received.sortBy(x => x.stopRecords.head.arrivalTime)(Ordering[Long].reverse)
      .map(x => (VehicleReg(x.journey.vehicleReg), x.source, x.stopRecords.sortBy(_.seqNo)
        .map(x => SequenceNo(x.seqNo) -> ArrivalTime(x.arrivalTime)).toMap))
  }

  def getStopArrivalHistory(busStopID: String): List[(Journey, lbtweb.Source, ArrivalTime)] = {
    //TODO get stop name
    val res = Source.fromURL(serverPrefix + "stop/" + busStopID).mkString
    val received = parse(res).extract[List[IncomingHistoricalStopRecord]]
    received
      .map(x => (x.journey, x.source, ArrivalTime(x.arrivalTime)))
      .sortBy(x => x._3.value)(Ordering[Long].reverse)
  }

  def getVehicleArrivalHistory(vehicleID: String): List[(BusRoute, lbtweb.Source, List[(SequenceNo, BusStop, ArrivalTime)])] = {
    val res = Source.fromURL(serverPrefix + "vehicle/" + vehicleID).mkString
    val received = parse(res).extract[List[IncomingHistoricalJourneyRecord]]
    assert(!received.exists(x => x.journey.vehicleReg != vehicleID))
    received
      .map(x => (x.journey.busRoute, x.source, x.stopRecords.sortBy(_.arrivalTime)(Ordering[Long].reverse)))
      .sortBy(x => x._3.head.arrivalTime)(Ordering[Long].reverse)
      .map(y => (y._1, y._2, y._3.map(z => (SequenceNo(z.seqNo), z.busStop, ArrivalTime(z.arrivalTime)))))
  }

  def getRouteArrivalHistoryStats(busRoute: BusRoute): Map[SequenceNo, Stats] = {
    val res = Source.fromURL(serverPrefix + "busroute/" + busRoute.name + "/" + busRoute.direction).mkString
    val received = parse(res).extract[List[IncomingHistoricalJourneyRecord]]
    assert(!received.exists(x => x.journey.busRoute != busRoute))
    val timeDiffBetweenAllStops = for {
      seqNoArrTime <- received.map(x => x.stopRecords.map(y => (y.seqNo, y.arrivalTime)))
      seqNoArrTimeWithoutLast = seqNoArrTime.dropRight(1)
      result = seqNoArrTimeWithoutLast.dropRight(1).map(seq => SequenceNo(seq._1)) zip (seqNoArrTimeWithoutLast drop 1, seqNoArrTimeWithoutLast).zipped.map(_._2 - _._2)
    } yield result
    timeDiffBetweenAllStops.flatten.groupBy(_._1).mapValues(x => Stats(new Duration(x.map(_._2).avg.toInt), new Duration(x.map(_._2).min.toInt), new Duration(x.map(_._2).max.toInt)))
  }

  def getStopToStopArrivalHistoryStats(busRoute: BusRoute, fromStopID: String, toStopID: String, fromSecOfWeek: Option[Int], toSecOfWeek: Option[Int]): Option[Stats] = {
    val res = Source.fromURL(serverPrefix + "busroute/" + busRoute.name + "/" + busRoute.direction + "?fromStopID=" + fromStopID + "&toStopID=" + toStopID + "&fromArrivalTimeSecOfWeek=" + fromSecOfWeek.getOrElse(0) + "&toArrivalTimeSecOfWeek=" + toSecOfWeek.getOrElse(604800)).mkString
    val received = parse(res).extract[List[IncomingHistoricalJourneyRecord]]
    println("RES: " + res)
    assert(!received.exists(x => x.journey.busRoute != busRoute))
    if (received.nonEmpty) {
      val nonEmptySequence = received.filter(x => x.stopRecords.head.busStop.stopID == fromStopID && x.stopRecords.last.busStop.stopID == toStopID)
      val timeDiffBetweenStops: Seq[(String, Long, Long)] = nonEmptySequence.map(x => (x.journey.vehicleReg, x.stopRecords.head.arrivalTime, x.stopRecords.last.arrivalTime - x.stopRecords.head.arrivalTime))
      val timeDifferences = timeDiffBetweenStops.map(x => x._3)
      if (timeDifferences.nonEmpty) Some(Stats(new Duration(timeDifferences.avg.toInt), new Duration(timeDifferences.min.toInt), new Duration(timeDifferences.max.toInt)))
      else None
    } else None
  }

  def doEndpointSpeedtest: Map[String, Long] = {
    Map(
      "getRoutes" -> speedTest("getRoutes"),
      "getStopList" -> speedTest("getStopList"),
      "getRouteArrivalHistoryData" -> speedTest("getRouteArrivalHistoryData"),
      "getStopArrivalHistory" -> speedTest("getStopArrivalHistory"),
      "getVehicleArrivalHistory" -> speedTest("getVehicleArrivalHistory"),
      "getRouteArrivalHistoryStats" -> speedTest("getRouteArrivalHistoryStats"),
      "getStopToStopArrivalHistoryStats" -> speedTest("getStopToStopArrivalHistoryStats")
    )
  }

  private def speedTest(method: String): Long = {
    val startTime: Long = System.currentTimeMillis()
    method match {
      case "getRoutes" => getRoutes
      case "getStopList" => getStopList(BusRoute("3", "outbound"))
      case "getRouteArrivalHistoryData" => getRouteArrivalHistoryData(BusRoute("3", "outbound"))
      case "getStopArrivalHistory" => getStopArrivalHistory("490000179F")
      case "getVehicleArrivalHistory" => getVehicleArrivalHistory("LTZ1627")
      case "getRouteArrivalHistoryStats" => getRouteArrivalHistoryStats(BusRoute("3", "outbound"))
      case "getStopToStopArrivalHistoryStats" => getStopToStopArrivalHistoryStats(BusRoute("3", "outbound"),"490008376S", "490008645X", Some(0), Some(604800))
    }
    System.currentTimeMillis() - startTime
  }
}