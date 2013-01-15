package justf0rfun.cybersoccer.launcher;
import java.io.File
import java.io.FileReader
import java.net.URL
import java.util.Properties
import scala.collection.JavaConversions
import akka.actor.ActorSystem
import akka.actor.Props
import justf0rfun.cybersoccer.centralnervoussystems.CentralNervousSystemFactory
import justf0rfun.cybersoccer.controller.MatchController
import justf0rfun.cybersoccer.gui.swing.CyberSoccerFrame
import justf0rfun.cybersoccer.model.MatchConfiguration
import justf0rfun.cybersoccer.model.SoccerField
import justf0rfun.cybersoccer.controller.PublishAndSubscribeProtocol
import akka.util.Duration
import java.util.concurrent.TimeUnit
import justf0rfun.cybersoccer.gui.swing.SoccerEventsFrame

object Launcher extends App {

	launch

	def launch = {
		val properties = readProperties
		val hostBrainFactory: CentralNervousSystemFactory = Class.forName(properties("soccer.match.host.factory")).newInstance.asInstanceOf[CentralNervousSystemFactory]
		val guestBrainFactory: CentralNervousSystemFactory = Class.forName(properties("soccer.match.guest.factory")).newInstance.asInstanceOf[CentralNervousSystemFactory]
		val matchConfiguration = configuration(properties)
		val actorSystem = ActorSystem("CyberSoccer")
		val cycleTimeInterval: Long = properties("soccer.match.cycleTimeInterval").toLong
		val matchController = actorSystem.actorOf(Props(new MatchController(hostBrainFactory, guestBrainFactory, matchConfiguration, cycleTimeInterval)) withDispatcher ("matchControllerDispatcher"), name = "matchController")
		val fieldView = actorSystem.actorOf(Props(new CyberSoccerFrame(matchConfiguration, matchController)), name = "soccerFieldView")
		//		val eventsView = actorSystem.actorOf(Props(new SoccerEventsFrame), name = "soccerEventsView")
		//		actorSystem.scheduler.schedule(Duration.Zero, Duration.create(100, TimeUnit.MILLISECONDS), gui, Update)
		matchController ! new PublishAndSubscribeProtocol.Subscribe(fieldView)
		//		matchController ! new PublishAndSubscribeProtocol.Subscribe(eventsView)
		MatchController.startMatch(matchController)
	}

	private def configuration(properties: Map[String, String]) = {
		val fieldWidth = properties("soccer.field.width").toDouble
		val ballRadius: Double = properties("soccer.match.ball.radius").toDouble
//		val ballRadius: Double = fieldWidth * .004 //properties("soccer.match.ball.radius").toDouble
		val ballFriction: Double = properties("soccer.match.ball.friction").toDouble
		val field = new SoccerField(fieldWidth, properties("soccer.field.height").toDouble, properties("soccer.field.goal.width").toDouble, ballRadius)
		val numberOfPlayers: Int = properties("soccer.match.numberOfTeamMembers").toInt
		val duration: Long = properties("soccer.match.duration").toLong
		val velocityMaximum: Double = properties("soccer.player.velocity.maximum").toDouble
		val kickForceMaximum: Double = properties("soccer.player.kick.force.maximum").toDouble
		val playerRangeRadius: Double = field.width * .006 //properties("soccer.player.rangeRadius").toDouble
		val kickAngleInaccuracyMaximum: Double = properties("soccer.player.kick.angle.inaccuracy.maximum").toDouble
		new MatchConfiguration(field, numberOfPlayers, duration, velocityMaximum, playerRangeRadius, ballRadius, ballFriction, kickForceMaximum, kickAngleInaccuracyMaximum)
	}

	private def readProperties: Map[String, String] = {
		//What about nice exception handling and how does propper file io look like in scala?
		val properties = new Properties()
		properties.load(getClass().getResourceAsStream("/cybersoccer.properties"))
		JavaConversions.propertiesAsScalaMap(properties).toMap
	}

}