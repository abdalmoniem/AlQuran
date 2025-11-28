package com.hifnawy.pre.build

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.io.IOException

/**
 * A sealed interface representing the base type for all possible errors within the plugin.
 * This is used in the [Result] type to provide a structured way of handling failures.
 *
 * @author AbdElMoniem ElHifnawy
 */
private sealed interface Error

/**
 * Represents the root error type for all errors within the plugin.
 *
 * @see Error
 */
private typealias RootError = Error

/**
 * A sealed interface representing the outcome of an operation, which can either be a [Success] or an [Error].
 * This is a generic type used to handle results from operations that might fail, such as executing external commands.
 *
 * @param D The type of the data returned on success. This is a covariant type parameter.
 * @param E The type of the error returned on failure. It must be a subtype of [RootError]. This is a covariant type parameter.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see RootError
 * @see Success
 * @see Error
 */
private sealed interface Result<out D, out E : RootError> {

    /**
     * Represents a successful outcome of an operation.
     * This class is part of the [Result] sealed interface and contains the successful result data.
     *
     * @param D The type of the successful data.
     * @property stdout [String] The data returned from a successful operation.
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see Result
     * @see Error
     */
    data class Success<out D>(val stdout: D) : Result<D, Nothing>

    /**
     * Represents a failed outcome of an operation.
     * This class is part of the [Result] sealed interface and contains the error information.
     *
     * @param E The type of the error, which must be a subtype of [RootError].
     * @property error [E] The specific error instance that occurred during the operation.
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see Result
     * @see Success
     */
    data class Error<out E : RootError>(val error: E) : Result<Nothing, E>
}

/**
 * Represents an error that occurs during the execution of an external command or process.
 * This class captures the exit code and the error message from the failed process.
 *
 * It is a concrete implementation of the [Error] sealed interface and is used within the [Result.Error] data class
 * to provide specific details about execution failures.
 *
 * @property errorCode [Int] The exit code returned by the failed process.
 * @property errorMessage [String] The error message or output captured from the process's error stream.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see Error
 * @see Result
 */
private class ExecutionError(val errorCode: Int, val errorMessage: String) : Error

/**
 * A data class that encapsulates the results of executing an external process.
 * It holds the process's exit code and the content of its standard output and standard error streams.
 *
 * @property exitCode [Int] The exit code of the process. A value of 0 typically indicates successful execution.
 * @property stdout [String] The text content captured from the process's standard output stream.
 * @property stderr [String] The text content captured from the process's standard error stream.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see Process
 * @see ProcessBuilder
 */
private data class ProcessMetadata(val exitCode: Int, val stdout: String, val stderr: String)

/**
 * A data class that encapsulates file path information related to the helper scripts.
 *
 * This class provides convenient access to the directory containing the Python helper scripts
 * and the Python virtual environment (`.venv`) directory within it. The paths are resolved
 * relative to the project's root directory.
 *
 * @param project [Project] The Gradle [Project] instance, used to resolve the root directory.
 * @property dir [File] A [File] object representing the `helper-scripts` directory located at the project root.
 * @property venv [File] A [File] object representing the `.venv` directory inside the `helper-scripts` directory.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see Project
 * @see File
 */
private data class HelperScripts(private val project: Project) {

    val dir = File(project.rootDir, "helper-scripts")
    val venv = File(dir, ".venv")
}

/**
 * A Gradle extension class that provides access to the tasks defined by the [PreBuildPlugin].
 *
 * This class is registered as a Gradle extension named `preBuildPlugin` within the project.
 * It allows build scripts (e.g., `build.gradle.kts`) to easily access and configure the tasks
 * for generating sample data and surah drawables. The properties in this class are populated
 * by the [PreBuildPlugin.apply] method with the corresponding [TaskProvider] instances.
 *
 * Example usage in `build.gradle.kts`:
 * ```kotlin
 * // Make another task depend on the sample data generation
 * tasks.named("someOtherTask") {
 *     dependsOn(preBuildPlugin.generateSampleData, preBuildPlugin.generateSurahDrawables)
 * }
 * ```
 *
 * @property generateSampleData [TaskProvider < Task >][TaskProvider] A [TaskProvider] for the `generateSampleData` task, which runs a Python script to generate sample data.
 * @property generateSurahDrawables [TaskProvider < Task >][TaskProvider] A [TaskProvider] for the `generateSurahDrawables` task, which runs a Python script to generate drawable assets.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see PreBuildPlugin
 * @see TaskProvider
 * @see Task
 */
open class PreBuildPluginEx {

    /**
     * A [TaskProvider] for the `generateSampleData` task.
     *
     * This task executes the `generateSampleData.py` Python script located in the `helper-scripts` directory.
     * It is responsible for generating sample data files required by the application. The task ensures that the
     * Python virtual environment is set up and its dependencies are synchronized before running the script.
     *
     * This provider allows other Gradle tasks to declare a dependency on the sample data generation process,
     * ensuring it runs at the appropriate time during the build.
     *
     * @return A [TaskProvider < Task >][TaskProvider] for the `generateSampleData` task.
     *
     * @see PreBuildPlugin.apply
     * @see TaskProvider
     */
    lateinit var generateSampleData: TaskProvider<Task>

    /**
     * A [TaskProvider] for the `generateSurahDrawables` task. This task executes the `generateSurahDrawables.py`
     * script, which is responsible for creating drawable image assets for each Surah (chapter) of the Quran.
     * These drawables are typically used within the application's UI to visually represent each Surah.
     *
     * The task ensures that the Python virtual environment is set up and its dependencies are synchronized
     * before running the script.
     *
     * @return A [TaskProvider < Task >][TaskProvider] for the `generateSurahDrawables` task.
     *
     * @see PreBuildPlugin
     * @see TaskProvider
     */
    lateinit var generateSurahDrawables: TaskProvider<Task>
}

/**
 * A Gradle plugin that automates the execution of Python scripts as part of the build process.
 *
 * This plugin is designed to manage a Python virtual environment using `uv`, a fast Python package installer and resolver.
 * It sets up a series of tasks to ensure the environment is created and its dependencies are synchronized before running
 * specific Python scripts for code generation.
 *
 * The plugin registers the following tasks:
 * - `checkUv`: Verifies that `uv` is installed and available in the system's PATH.
 * - `createVenv`: Creates a Python virtual environment in the `helper-scripts/.venv` directory if it doesn't already exist.
 * - `syncVenv`: Synchronizes the virtual environment's dependencies based on a `requirements.txt` file using `uv sync`.
 * - `generateSampleData`: Executes the `generateSampleData.py` script to generate necessary sample data files.
 * - `generateSurahDrawables`: Executes the `generateSurahDrawables.py` script to create drawable assets.
 *
 * It also provides a [PreBuildPluginEx] extension named `preBuildPlugin`, allowing other parts of the build script
 * to easily access and depend on the `generateSampleData` and `generateSurahDrawables` tasks.
 *
 * This plugin simplifies the integration of Python-based helper scripts into a Gradle-based project,
 * ensuring that build-time dependencies and code generation steps are handled automatically and reliably.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see Plugin
 * @see Project
 * @see PreBuildPluginEx
 */
@Suppress("LoggingSimilarMessage")
class PreBuildPlugin : Plugin<Project> {

    /**
     * Applies the Pre-Build plugin to a given [Project].
     *
     * This method is the entry point for the plugin. It performs the following actions:
     * 1. Creates and registers the `preBuildPlugin` extension of type [PreBuildPluginEx], making it available in build scripts.
     * 2. Defines file paths for the helper scripts directory, the Python virtual environment (`.venv`), and the Python scripts.
     * 3. Registers a sequence of Gradle tasks:
     *    - `checkUv`: Verifies that the `uv` tool is installed.
     *    - `createVenv`: Creates a Python virtual environment if one doesn't exist, depending on `checkUv`.
     *    - `syncVenv`: Synchronizes the virtual environment's dependencies using `requirements.txt`, depending on `createVenv`.
     *    - `generateSampleData`: Runs the `generateSampleData.py` script, depending on `syncVenv`.
     *    - `generateSurahDrawables`: Runs the `generateSurahDrawables.py` script, depending on `syncVenv`.
     * 4. Populates the `preBuildPlugin` extension with [TaskProvider] instances for the `generateSampleData` and `generateSurahDrawables` tasks,
     *    allowing other tasks to easily declare dependencies on them.
     *
     * Each task is configured to execute a shell command using `uv` and handles success and failure cases by logging output
     * or throwing a [GradleException].
     *
     * @param project [Project] The Gradle [Project] to which this plugin is being applied.
     */
    override fun apply(project: Project) = project.run {
        val helperScripts = HelperScripts(this@run)

        val checkUvTask = registerCommandTask(
                name = "checkUv",
                dependsOn = emptyList(),
                workingDir = helperScripts.dir,
                command = "uv --version",
                errorMessage = "uv is not installed or not found in PATH. Please install uv: https://docs.astral.sh/uv/getting-started/installation/"
        )

        val createVenvTask = registerCommandTask(
                name = "createVenv",
                dependsOn = listOf(checkUvTask),
                workingDir = helperScripts.dir,
                command = "uv venv .venv",
                errorMessage = "Failed to create venv",
                onlyIf = { !helperScripts.venv.exists() }
        )

        val syncVenvTask = registerCommandTask(
                name = "syncVenv",
                dependsOn = listOf(createVenvTask),
                workingDir = helperScripts.dir,
                command = "uv sync",
                errorMessage = "Failed to sync venv"
        )

        val generateSampleDataTask = registerCommandTask(
                name = "generateSampleData",
                dependsOn = listOf(syncVenvTask),
                workingDir = helperScripts.dir,
                command = "uv run generateSampleData.py"
        )

        val generateSurahDrawablesTask = registerCommandTask(
                name = "generateSurahDrawables",
                dependsOn = listOf(syncVenvTask),
                workingDir = helperScripts.dir,
                command = "uv run generateSurahDrawables.py --headless"
        )

        extensions.create("preBuildPlugin", PreBuildPluginEx::class.java).run {
            generateSampleData = generateSampleDataTask
            generateSurahDrawables = generateSurahDrawablesTask
        }
    }

    /**
     * Registers a new Gradle task that executes a shell command.
     *
     * This is a helper extension function for [Project] that simplifies the creation of tasks
     * designed to run external commands. The function handles task registration, dependency management,
     * conditional execution, and command output processing.
     *
     * The created task will execute the specified [command] in the given [workingDir].
     * If the command succeeds (exit code 0), its standard output is logged at the `LIFECYCLE` level.
     * If the command fails (non-zero exit code), the standard error is logged as an `ERROR`, and a [GradleException]
     * is thrown, causing the build to fail.
     *
     * @param name [String] The name for the new task to be registered.
     * @param dependsOn [List]<[TaskProvider]<out [Task]>> A list of [TaskProvider]s that this new task will depend on. The task will not run until all dependencies have completed successfully.
     * @param workingDir [File] The working directory in which the [command] will be executed.
     * @param command [String] The shell command to execute (e.g., "uv --version").
     * @param errorMessage [String?] An optional custom error message to be used in the [GradleException] if the command fails. If `null`, the command's standard error output will be used.
     * @param onlyIf [(() -> Boolean)?][(() -> Boolean)] An optional predicate. If provided, the task will only execute if the predicate returns `true`.
     *
     * @return [TaskProvider< Task >][TaskProvider] A [TaskProvider] for the newly registered task.
     *
     * @see executeCommand
     * @see GradleException
     */
    private fun Project.registerCommandTask(
            name: String,
            dependsOn: List<TaskProvider<out Task>>,
            workingDir: File,
            command: String,
            errorMessage: String? = null,
            onlyIf: (() -> Boolean)? = null
    ) = tasks.register(name) {
        if (dependsOn.isNotEmpty()) dependsOn(dependsOn)

        onlyIf?.let { onlyIf { it() } }

        doLast {
            val result = executeCommand(workingDir, command)

            with(result) {
                when (this) {
                    is Result.Success if stdout.trim().isNotEmpty() -> logger.lifecycle(stdout.trim())
                    is Result.Success                        -> Unit
                    is Result.Error                          -> {
                        logger.error("ERROR ${error.errorCode}: ${error.errorMessage}")
                        throw GradleException(errorMessage ?: error.errorMessage)
                    }

                }
            }
        }
    }

    /**
     * Executes a shell command and captures its output.
     *
     * This function takes a command string, splits it into arguments, and executes it as an external process.
     * It uses a [processBuilder] to manage the process execution and captures the standard output, standard error,
     * and exit code.
     *
     * The execution result is wrapped in a [Result] type. If the command executes successfully (i.e., returns an
     * exit code of 0), it returns a [Result.Success] containing the trimmed standard output. If the command fails
     * (returns a non-zero exit code) or an [IOException] occurs (e.g., the command is not found), it returns a
     * [Result.Error] containing an [ExecutionError] with the exit code and error message.
     *
     * @param workingDir [File?][File] The working directory for the command execution. If `null`, the working directory of the
     *                   current Java process is used. Defaults to `null`.
     * @param command [String] The command to execute, including its arguments, as a single string (e.g., "uv --version").
     *
     * @return [Result<String, ExecutionError>][Result] A [Result] which is either a [Result.Success] with the command's standard output as a [String],
     *         or a [Result.Error] with an [ExecutionError] instance detailing the failure.
     *
     * @see Result
     * @see ExecutionError
     * @see processBuilder
     */
    private fun executeCommand(workingDir: File? = null, command: String): Result<String, ExecutionError> {
        return try {
            val commandWithArgs = command.split(" ").toTypedArray()
            val (exitCode, stdout, stderr) = processBuilder(workingDir = workingDir, args = commandWithArgs)

            when (exitCode) {
                0 -> Result.Success(stdout = stdout.trim())
                else -> Result.Error(ExecutionError(errorCode = exitCode, errorMessage = stderr.trim()))
            }
        } catch (ex: IOException) {
            Result.Error(ExecutionError(errorCode = -2, errorMessage = ex.message.toString()))
        }
    }

    /**
     * Executes an external command using [ProcessBuilder] and captures its output.
     *
     * This function configures and starts a new process to run the specified command. It redirects the
     * process's standard output and error streams to be captured as strings. The function waits for the
     * process to complete and then returns a [ProcessMetadata] object containing the exit code,
     * standard output, and standard error.
     *
     * @param workingDir [File?][File] An optional [File] representing the working directory for the process. If `null`,
     *                   the working directory of the current Java process is used.
     * @param args [vararg String][String] A variable number of strings representing the command and its arguments to be executed.
     *
     * @return A [ProcessMetadata] instance containing the process's exit code, stdout, and stderr.
     *
     * @see ProcessBuilder
     * @see ProcessMetadata
     * @see Process.waitFor
     */
    private fun processBuilder(workingDir: File? = null, vararg args: String): ProcessMetadata {
        val processBuilder = ProcessBuilder(*args)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .apply {
                environment().putAll(System.getenv())
                workingDir?.let { directory(it) }
            }

        val process = processBuilder.start()

        val stdout = process.inputStream.bufferedReader().use { it.readText() }.trim()
        val stderr = process.errorStream.bufferedReader().use { it.readText() }.trim()
        val exitCode = process.waitFor()

        return ProcessMetadata(exitCode, stdout, stderr)
    }
}
