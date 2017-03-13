package lbtweb

case class Start()
case class Stop()

case class BusStop(id: String, name: String, longitude: Double, latitude: Double)

case class BusRoute(id: String, direction: String)

case class BusRouteWithTowards(id: String, direction: String, towards: String)

case class IncomingHistoricalRecord(busRoute: BusRoute, vehicleID: String, stopRecords: List[IncomingVehicleStopRecord])
case class IncomingVehicleStopRecord(seqNo: Int, stopID: String, stopName: String, arrivalTime: Long)

object Commons {

  def toDirection(directionInt: Int): String = {
    directionInt match {
      case 1 => "outbound"
      case 2 => "inbound"
      case _ => throw new IllegalStateException(s"Unknown direction for string $directionInt")
    }
  }
}

