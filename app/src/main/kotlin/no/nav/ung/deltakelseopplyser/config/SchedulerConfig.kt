package no.nav.ung.deltakelseopplyser.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar

@EnableScheduling
@Configuration
class SchedulerConfig : SchedulingConfigurer {

    private val log = LoggerFactory.getLogger("ScheduledTask")

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 1
        scheduler.setThreadNamePrefix("scheduled-")
        scheduler.setErrorHandler { ex ->
            log.warn("Scheduled task failed: ${ex.message}", ex)
        }
        scheduler.initialize()
        taskRegistrar.setTaskScheduler(scheduler)
    }
}
