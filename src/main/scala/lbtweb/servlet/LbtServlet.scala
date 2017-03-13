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

  get("/routearrivalhistory/:routeID/:direction") {
    val busRoute = BusRoute(params("routeID"), params("direction"))
    val routeHistory = controllers.getRouteArrivalHistory(busRoute)
    val stopDefList = controllers.getStopList(busRoute)
    contentType="text/html"
    ssp("route-arrival-history-table", "title" -> s"Route Arrival List for ${busRoute.id}, ${busRoute.direction}", "busRoute" -> busRoute, "stopDefList" -> stopDefList, "routeArrivalList" -> routeHistory)
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

}