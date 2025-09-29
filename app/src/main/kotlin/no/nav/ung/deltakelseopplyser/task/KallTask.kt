package no.nav.ung.deltakelseopplyser.task

import no.nav.familie.prosessering.internal.TaskService
import org.springframework.stereotype.Service

@Service
class KallTask(
    private val taskService: TaskService
) {
    fun opprettTask(data: String) {
        taskService.save(DefinertTask.opprettTask(data))
    }
}
