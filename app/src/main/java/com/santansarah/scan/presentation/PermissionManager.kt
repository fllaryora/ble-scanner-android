package com.santansarah.scan.presentation

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.LifecycleOwner
import com.santansarah.scan.utils.permissionsArray
import org.koin.core.component.KoinComponent
import timber.log.Timber

class PermissionManager(
    private val activity: ComponentActivity
): KoinComponent {

    private val registry: ActivityResultRegistry = activity.activityResultRegistry
    private var btEnableResultLauncher = registerLauncher(activity, "BlePermissions")

    private fun launchPermissionCheck() {
        btEnableResultLauncher.launch(permissionsArray)
    }

    private fun registerLauncher(owner: LifecycleOwner, key: String) = registry.register(
        key,
        owner,
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, @JvmSuppressWildcards Boolean> ->
        // Handle Permission granted/rejected
        permissions.entries.forEach { stringBooleanEntry ->
            Timber.d(stringBooleanEntry.toString())
            val permissionName = stringBooleanEntry.key
            val isGranted = stringBooleanEntry.value
            if (isGranted) {
                // Permission is granted
            } else {
                // Permission is denied
            }
        }

    }

}
