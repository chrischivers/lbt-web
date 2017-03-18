package lbtweb


import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

case class Start()
case class Stop()

case class BusStop(id: String, name: String, longitude: Double, latitude: Double)
case class BusRoute(id: String, direction: String)
case class BusRouteWithTowards(id: String, direction: String, towards: String)

case class IncomingHistoricalRecord(busRoute: BusRoute, vehicleID: String, stopRecords: List[IncomingVehicleStopRecord])
case class IncomingVehicleStopRecord(seqNo: Int, busStop: BusStop, arrivalTime: Long)

//case class TimeParameters(dayOfWeek: Int, hourBeginning: Int)
case class Stats(average: Int, min: Int, max: Int)
//case class TimePeriodStats(timeParamsToStats: Map[TimeParameters, Stats])
//case class RouteTimePeriodStats(stopIDToTimePeriodStats: Map[String, TimePeriodStats])

case class VehicleReg(value: String)

case class ArrivalTime(value: Long) {
  private val dtf: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss")
  def toFormattedString: String = dtf.print(value)
}
case class SequenceNo(value: Int)

object Commons {

  def toDirection(directionInt: Int): String = {
    directionInt match {
      case 1 => "outbound"
      case 2 => "inbound"
      case _ => throw new IllegalStateException(s"Unknown direction for string $directionInt")
    }
  }

  def average[T]( ts: Iterable[T] )( implicit num: Numeric[T] ) = {
    num.toDouble( ts.sum ) / ts.size
  }
}

