

    fun getStatus(): String {
        return when (state.value) {
            is TunState.Idle -> "Idle"
            is TunState.Registering -> "Registering"
            is TunState.Starting -> "Starting"
            is TunState.Running -> "Running"
            is TunState.Stopped -> "Stopped"
            is TunState.Error -> "Error"
        }
    }
1