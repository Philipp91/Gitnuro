package com.jetpackduba.gitnuro.app.git

import com.jetpackduba.gitnuro.credentials.GProcess
import com.jetpackduba.gitnuro.credentials.GRemoteSession
import com.jetpackduba.gitnuro.credentials.GSessionManager
import com.jetpackduba.gitnuro.git.remote_operations.CloneRepositoryUseCase
import com.jetpackduba.gitnuro.git.remote_operations.HandleTransportUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL
import java.io.File
import kotlin.io.path.createTempDirectory

private const val REPO_URL = "https://github.com/JetpackDuba/Gitnuro_TestsRepo.git"
private val tempDirPath = createTempDirectory("gitnuro_")
val tempDir: File = tempDirPath.toFile()
lateinit var repoDir: File

class BeforeRepoAllTestsExtension : BeforeAllCallback, AfterAllCallback {
    private var started = false

    override fun beforeAll(context: ExtensionContext) = runBlocking {
        if (!started) {
            repoDir = File(tempDir, "repo")

            started = true

            // The following line registers a callback hook when the root test context is shut down
            context.root.getStore(GLOBAL).put("gitnuro_tests", this)

            val cloneRepositoryUseCase =
                CloneRepositoryUseCase(HandleTransportUseCase(GSessionManager { GRemoteSession { GProcess() } }))
            cloneRepositoryUseCase(repoDir, REPO_URL)
                .flowOn(Dispatchers.IO)
                .collect { newCloneStatus ->
                    println("Clonning test repository: $newCloneStatus")
                }
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        tempDir.deleteRecursively()
    }
}
