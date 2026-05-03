import java.util.concurrent.TimeUnit
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState

internal class KonitorExecutionTimePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (!Env.CI) return
        project.gradle.taskGraph.addTaskExecutionListener(TimingsListener())
    }
}

private class TimingsListener : TaskExecutionListener {
    private var startTime = 0L

    override fun beforeExecute(task: Task) {
        startTime = System.nanoTime()
    }

    override fun afterExecute(task: Task, state: TaskState) {
        val ms = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
        task.project.logger.warn("${task.path} took $ms ms")
    }
}
