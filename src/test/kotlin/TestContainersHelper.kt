import TestContainersHelper.wiremockContainerHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy
import org.testcontainers.images.builder.ImageFromDockerfile
import kotlin.io.path.Path

private val network = Network.newNetwork()
private var log: Logger = LoggerFactory.getLogger(TestContainersHelper::class.java)
private val imageFromDockerfile = ImageFromDockerfile().withDockerfile(Path("./Dockerfile"))

object TestContainersHelper {
	val kafkaContainerHelper = KafkaContainerHelper(network, log)
	val wiremockContainerHelper = WiremockContainerHelper()
	val fullEksportContainer = jobbContainer(lastNedAlleVirksomheter = true, kafkaContainerHelper)
	val oppdateringContainer = jobbContainer(lastNedAlleVirksomheter = false, kafkaContainerHelper)
}

private fun jobbContainer(
	lastNedAlleVirksomheter: Boolean = false,
	kafkaContainerHelper: KafkaContainerHelper,
) = GenericContainer(imageFromDockerfile)
		.dependsOn(
			kafkaContainerHelper.kafkaContainer
		)
		.withNetwork(network)
		.withLogConsumer(Slf4jLogConsumer(log).withPrefix("job").withSeparateOutputStreams())
		.withCreateContainerCmdModifier { cmd -> cmd.withName("job-${System.currentTimeMillis()}") }
		.withEnv(
			mapOf(
				"LAST_NED_ALLE_VIRKSOMHETER" to lastNedAlleVirksomheter.toString()
			).plus(
				kafkaContainerHelper.envVars()
			).plus(
				wiremockContainerHelper.envVars()
			)
		).waitingFor(
			OneShotStartupWaitStrategy()
		)

class OneShotStartupWaitStrategy: AbstractWaitStrategy() {
	override fun waitUntilReady() {
		if (!OneShotStartupCheckStrategy().waitUntilStartupSuccessful(
				waitStrategyTarget.dockerClient,
				waitStrategyTarget.containerId
			)
		) {
			throw Exception("Fikk ikke kjørt jobb ${waitStrategyTarget.containerId}")
		}
	}
}