package lbtweb.servlet

import com.typesafe.scalalogging.StrictLogging
import lbtweb._
import lbtweb.controllers.Controllers
import org.scalatra.Ok

class LbtServlet extends LbtwebStack with StrictLogging {

  val controllers = new Controllers(ConfigLoader.defaultConfig.serverConfig)

  get("/") {
    redirect("/routes")
  }
  get("/routes") {
    contentType = "text/html"
    val routeDirectionsTowards = controllers.getRoutes
     ssp("routes", "title" -> "Route List", "busRouteList" -> routeDirectionsTowards)
    }

    get("/route/:routeID/:direction") {
      val busRoute = BusRoute(params("routeID"), params("direction"))
      val stopList = controllers.getStopList(busRoute)
      contentType="text/html"
      ssp("route", "title" -> s"Route ${params("routeID")}, ${params("direction")}", "busRoute" -> busRoute, "stopList" -> stopList)
    }

  get("/routearrivalhistorydata/:routeID/:direction") {
    val busRoute = BusRoute(params("routeID"), params("direction"))
    val stopDefList = controllers.getStopList(busRoute)
    val routeHistoryData = controllers.getRouteArrivalHistoryData(busRoute)
    contentType="text/html"
    ssp("route-arrival-history-data", "title" -> s"Route Arrival List for ${busRoute.id}, ${busRoute.direction}", "busRoute" -> busRoute, "stopDefList" -> stopDefList, "routeArrivalList" -> routeHistoryData)
  }

  get("/stoparrivalhistory/:stopID") {
    val stopID = params("stopID")
    val stopHistory = controllers.getStopArrivalHistory(stopID)
    val stop = stopHistory.headOption match {
      case Some(head) => controllers.getStopList(head._1).find(stop => stop.id == stopID)
      case None => None
    }
    contentType="text/html"
    ssp("stop-arrival-history", "title" -> s"Stop Arrival History for ${stop.map(_.name).getOrElse("Stop name not found")} ($stopID)", "busStopOpt" -> stop, "stopArrivalList" -> stopHistory)
  }

  get("/vehiclearrivalhistory/:vehicleID") {
    val vehicleID = params("vehicleID")
    val vehicleHistory = controllers.getVehicleArrivalHistory(vehicleID)
    contentType="text/html"
    ssp("vehicle-arrival-history", "title" -> s"Vehicle Arrival History List for $vehicleID", "vehicleArrivalList" -> vehicleHistory)
  }

  get("/routearrivalhistorystats/:routeID/:direction") {
    val busRoute = BusRoute(params("routeID"), params("direction"))
    val stopDefList = controllers.getStopList(busRoute)
    val routeHistoryStats = controllers.getRouteArrivalHistoryStats(busRoute)
    contentType="text/html"
    ssp("route-arrival-history-stats", "title" -> s"Route Arrival Stats for ${busRoute.id}, ${busRoute.direction}", "busRoute" -> busRoute, "stopDefList" -> stopDefList, "routeArrivalList" -> routeHistoryStats)
  }

  get("/stopToStopArrivalHistoryStats/:routeID/:direction/:fromStopID/:toStopID") {
    val busRoute = BusRoute(params("routeID"), params("direction"))
    val fromStopID = params("fromStopID")
    val toStopID = params("toStopID")
    val stopDefList = controllers.getStopList(busRoute)
    val stopToStopHistoryStats = controllers.getStopToStopArrivalHistoryStats(busRoute, fromStopID, toStopID)
    contentType="text/html"
    ssp("route-arrival-history-stats", "title" -> s"Route Arrival Stats for ${busRoute.id}, ${busRoute.direction} from $fromStopID to $toStopID", "busRoute" -> busRoute, "fromStop" -> stopDefList.find(x => x.id == fromStopID).getOrElse("N/A"), "toStop" -> stopDefList.find(x => x.id == toStopID).getOrElse("N/A"), "routeArrivalList" -> stopToStopHistoryStats)
  }

}