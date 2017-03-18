package lbtweb.controllers

import com.typesafe.scalalogging.StrictLogging
import lbtweb._
import net.liftweb.json._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.collection.immutable.Seq
import scala.io.Source

class Controllers(config: ServerConfig) extends StrictLogging {

  implicit def iterebleWithAvg[T: Numeric](data: Iterable[T]) = new {
    def avg = Commons.average(data)
  }


  implicit val formats = DefaultFormats
  val serverPrefix = config.url + ":" + config.port + "/historical/"

  def getRoutes: List[(String, List[(String, String)])] = {
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

  def getRouteArrivalHistoryData(busRoute: BusRoute): List[(VehicleReg, Map[SequenceNo, ArrivalTime])] = {
    val res = Source.fromURL(serverPrefix + "busroute/" + busRoute.id + "/" + busRoute.direction).mkString
    val received = parse(res).extract[List[IncomingHistoricalRecord]]
    assert(!received.exists(x => x.busRoute != busRoute))
    received.sortBy(x => x.stopRecords.head.arrivalTime)(Ordering[Long].reverse)
      .map(x => (VehicleReg(x.vehicleID), x.stopRecords.sortBy(_.seqNo)
        .map(x => SequenceNo(x.seqNo) -> ArrivalTime(x.arrivalTime)).toMap))
  }

  def getStopArrivalHistory(busStopID: String): List[(BusRoute, VehicleReg, ArrivalTime)] = {
    val res = Source.fromURL(serverPrefix + "stop/" + busStopID).mkString
    val received = parse(res).extract[List[IncomingHistoricalRecord]]
    received
      .map(x => (x.busRoute, x.vehicleID, x.stopRecords.find(y => y.busStop.id == busStopID).get.arrivalTime))
      .sortBy(x => x._3)(Ordering[Long].reverse)
      .map(x => (x._1, VehicleReg(x._2), ArrivalTime(x._3)))
  }

  def getVehicleArrivalHistory(vehicleID: String): List[(BusRoute, List[(SequenceNo, BusStop, ArrivalTime)])] = {
    val res = Source.fromURL(serverPrefix + "vehicle/" + vehicleID).mkString
    val received = parse(res).extract[List[IncomingHistoricalRecord]]
    assert(!received.exists(x => x.vehicleID != vehicleID))
    received
      .map(x => (x.busRoute, x.stopRecords.sortBy(_.arrivalTime)(Ordering[Long].reverse)))
      .sortBy(x => x._2.head.arrivalTime)(Ordering[Long].reverse)
      .map(y => (y._1, y._2.map(z => (SequenceNo(z.seqNo), z.busStop, ArrivalTime(z.arrivalTime)))))
  }

  def getRouteArrivalHistoryStats(busRoute: BusRoute): Map[SequenceNo, Stats] = {
    val res = Source.fromURL(serverPrefix + "busroute/" + busRoute.id + "/" + busRoute.direction).mkString
    val received = parse(res).extract[List[IncomingHistoricalRecord]]
    assert(!received.exists(x => x.busRoute != busRoute))
    val timeDiffBetweenAllStops = for {
      seqNoArrTime <- received.map(x => x.stopRecords.map(y => (y.seqNo, y.arrivalTime)))
      seqNoArrTimeWithoutLast = seqNoArrTime.dropRight(1)
      result = seqNoArrTimeWithoutLast.dropRight(1).map(seq => SequenceNo(seq._1)) zip (seqNoArrTimeWithoutLast drop 1, seqNoArrTimeWithoutLast).zipped.map(_._2 - _._2)
    } yield result
    timeDiffBetweenAllStops.flatten.groupBy(_._1).mapValues(x => Stats((x.map(_._2).avg / 1000).toInt, (x.map(_._2).min / 1000).toInt, (x.map(_._2).max / 1000).toInt))
  }

  def getStopToStopArrivalHistoryStats(busRoute: BusRoute, fromStopID: String, toStopID: String): Stats = {
    val res = Source.fromURL(serverPrefix + "busroute/" + busRoute.id + "/" + busRoute.direction + "?fromStopID=" + fromStopID + "&toStopID=" + toStopID).mkString
    val received = parse(res).extract[List[IncomingHistoricalRecord]]
    assert(!received.exists(x => x.busRoute != busRoute))
    assert(!received.exists(x => x.stopRecords.exists(y => y.busStop.id != fromStopID)))
    assert(!received.exists(x => x.stopRecords.exists(y => y.busStop.id != toStopID)))
    assert(!received.exists(x => x.stopRecords.head.busStop.id != fromStopID))
    assert(!received.exists(x => x.stopRecords.last.busStop.id != toStopID))
    val timeDiffBetweenStops: Seq[(String, Long, Long)] = received.map(x => (x.vehicleID, x.stopRecords.head.arrivalTime, x.stopRecords.last.arrivalTime - x.stopRecords.head.arrivalTime))
    val timeDifferences = timeDiffBetweenStops.map(x => x._3)
    Stats((timeDifferences.avg / 1000).toInt, (timeDifferences.min / 1000).toInt, (timeDifferences.max / 1000).toInt)
  }
}