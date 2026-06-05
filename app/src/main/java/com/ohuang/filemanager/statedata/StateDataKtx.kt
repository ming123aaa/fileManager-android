package com.ohuang.filemanager.statedata


import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> StateData<T>.addOnceAlwaysObserverForSticky(
    lifecycleOwner: LifecycleOwner,
    call: (T) -> Unit
): Observer<T> {
    val observer = object : Observer<T> {
        override fun onchange(t: T) {
            call(t)
            this@addOnceAlwaysObserverForSticky.removeObserver(this)
        }
    }
    addAlwaysObserverForSticky(lifecycleOwner, observer)
    return observer
}



fun <T> LiveData<T>.toStateDate(): StateData<T> {
    val stateData = StateData<T>()
    stateData.value = value
    observeForever {
        stateData.value = it
    }

    return stateData
}

fun <T> StateData<T>.toLiveData(): LiveData<T> {
    val mutableLiveData = MutableLiveData<T>()
    if (value != null) {
        mutableLiveData.value = value
    }
    addObserver {
        mutableLiveData.value = it
    }

    return mutableLiveData
}

fun <T> Flow<T>.toStateData(context: CoroutineContext = EmptyCoroutineContext): StateData<T> {
    val stateData = StateData<T>()
    CoroutineScope(context).launch() {
        collect {
            launch(Dispatchers.Main) {
                stateData.value = it
            }
        }
    }
    return stateData
}

@OptIn(DelicateCoroutinesApi::class)
fun <T> StateData<T>.asFlow(): Flow<T> = callbackFlow {
    val observer = Observer<T> {
        trySend(it)
    }
    withContext(Dispatchers.Main) {
        addObserver (observer)
    }

    awaitClose {
        GlobalScope.launch(Dispatchers.Main) {
            removeObserver(observer)
        }
    }
}.conflate()