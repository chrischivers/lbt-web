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
    logger.info("/routes request received")
    contentType = "text/html"
    val routeDirectionsTowards = controllers.getRoutes
     ssp("routes", "title" -> "Route List", "busRouteList" -> routeDirectionsTowards)
    }

    get("/route/:routeID/:direction") {
      val busRoute = BusRoute(params("routeID"), params("direction"))
      val stopList = controllers.getStopList(busRoute)
      logger.info(s"/route request received for $busRoute")
      contentType="text/html"
      ssp("route", "title" -> s"Route ${params("routeID")}, ${params("direction")}", "busRoute" -> busRoute, "stopList" -> stopList)
    }

  get("/routearrivalhistorydata/:routeID/:direction") {
    val busRoute = BusRoute(params("routeID"), params("direction"))
    val stopDefList = controllers.getStopList(busRoute)
    val routeHistoryData = controllers.getRouteArrivalHistoryData(busRoute)
    logger.info(s"/routearrivalhistorydata request received for $busRoute")
    contentType="text/html"
    ssp("route-arrival-history-data", "title" -> s"Route Arrival List for ${busRoute.name}, ${busRoute.direction}", "busRoute" -> busRoute, "stopDefList" -> stopDefList, "routeArrivalList" -> routeHistoryData)
  }

  get("/stoparrivalhistory/:stopID") {
    val stopID = params("stopID")
    val stopHistory: List[(Journey, lbtweb.Source, ArrivalTime)] = controllers.getStopArrivalHistory(stopID)
    logger.info(s"/stoparrivalhistory request received for $stopID")
    contentType="text/html"
    ssp("stop-arrival-history", "title" -> s"Stop Arrival History for $stopID", "stopArrivalList" -> stopHistory)
  }

  get("/vehiclearrivalhistory/:vehicleID") {
    val vehicleID = params("vehicleID")
    logger.info(s"/vehiclearrivalhistory request received for $vehicleID")
    val vehicleHistory = controllers.getVehicleArrivalHistory(vehicleID)
    contentType="text/html"
    ssp("vehicle-arrival-history", "title" -> s"Vehicle Arrival History List for $vehicleID", "vehicleArrivalList" -> vehicleHistory)
  }

  get("/routearrivalhistorystats/:routeID/:direction") {
    val busRoute = BusRoute(params("routeID"), params("direction"))
    val stopDefList = controllers.getStopList(busRoute)
    logger.info(s"/routearrivalhistorystats request received for $busRoute")
    val routeHistoryStats = controllers.getRouteArrivalHistoryStats(busRoute)
    contentType="text/html"
    ssp("route-arrival-history-stats", "title" -> s"Route Arrival Stats for ${busRoute.name}, ${busRoute.direction}", "busRoute" -> busRoute, "stopDefList" -> stopDefList, "routeArrivalList" -> routeHistoryStats)
  }

  get("/stopToStopArrivalHistoryStats/:routeID/:direction/:fromStopID/:toStopID") {
    val busRoute = BusRoute(params("routeID"), params("direction"))
    val fromStopID = params("fromStopID")
    val toStopID = params("toStopID")
    val fromSecOfWeek = params.get("fromSecOfWeek")
    val toSecOfWeek = params.get("toSecOfWeek")
    logger.info(s"/stopToStopArrivalHistoryStats request received for $busRoute, from $fromStopID, to $toStopID")
    val stopDefList = controllers.getStopList(busRoute)
    val stopToStopHistoryStatsOpt = controllers.getStopToStopArrivalHistoryStats(busRoute, fromStopID, toStopID, fromSecOfWeek.map(_.toInt), toSecOfWeek.map(_.toInt))
    contentType="text/html"
    ssp("route-arrival-history-stats-stop-to-stop", "title" -> s"Route Arrival Stats for ${busRoute.name}, ${busRoute.direction} from $fromStopID to $toStopID", "busRoute" -> busRoute, "fromStop" -> stopDefList.find(x => x.stopID == fromStopID).getOrElse("N/A"), "toStop" -> stopDefList.find(x => x.stopID == toStopID).getOrElse("N/A"), "stats" -> stopToStopHistoryStatsOpt)
  }

  get("/speedtest") {
    logger.info(s"/speedtest request received")
    val speedTestResults = controllers.doEndpointSpeedtest
    contentType="text/html"
    ssp("speedtest", "title" -> "Endpoint Speed Test", "speedTestMap" -> speedTestResults)
  }

}