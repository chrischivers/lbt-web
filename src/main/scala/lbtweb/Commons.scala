package lbtweb


import org.joda.time.{DateTime, Duration}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, PeriodFormatterBuilder}

case class Start()
case class Stop()

case class BusStop(stopID: String, stopName: String, longitude: Double, latitude: Double)
case class BusRoute(name: String, direction: String)
case class BusRouteWithTowards(name: String, direction: String, towards: String)
case class BusStopName(value: String)
case class Journey(busRoute: BusRoute, vehicleReg: String, startingTimeMillis: Long, startingSecondOfWeek: Int)
case class Source(value: String)

case class IncomingHistoricalJourneyRecord(journey: Journey, source: Source, stopRecords: List[IncomingHistoricalArrivalRecord])
case class IncomingHistoricalArrivalRecord(seqNo: Int, busStop: BusStop, arrivalTime: Long)
case class IncomingHistoricalStopRecord(stopID: String, arrivalTime: Long, journey: Journey, source: Source)

case class Stats(average: Duration, min: Duration, max: Duration)

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

  def durationToFormattedDuration(duration: Duration) = {
    val formatter = new PeriodFormatterBuilder()
      .appendHours()
      .appendSuffix("h ")
      .appendMinutes()
      .appendSuffix("m ")
      .appendSeconds()
      .appendSuffix("s ")
      .toFormatter
    formatter.print(duration.toPeriod())
  }
}

