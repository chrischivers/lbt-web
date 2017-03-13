package lbtweb.servlet

import lbtweb._
import lbtweb.controllers.Controllers
import org.scalatra.Ok

class LbtServlet extends LbtwebStack {

  val controllers = new Controllers(ConfigLoader.defaultConfig.serverConfig)

  get("/") {
    redirect("/routelist")
  }
  get("/routelist") {
    contentType = "text/html"
    val routeDirectionsTowards = controllers.getRouteList
     ssp("routelist", "title" -> "Route List", "busRouteList" -> routeDirectionsTowards)
    }

    get("/stoplist/:routeID/:direction") {
      val busRoute = BusRoute(params("routeID"), params("direction"))
      val stopList = controllers.getStopList(busRoute)
      contentType="text/html"
      ssp("stoplist", "title" -> s"Stop List for ${params("routeID")}, ${params("direction")}", "busRoute" -> busRoute, "stopList" -> stopList)
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
    contentType="text/html"
    ssp("stop-arrival-history", "title" -> s"Stop Arrival History for stopID $stopID", "busStopID" -> stopID, "stopArrivalList" -> stopHistory)
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

}