package com.mk.gpstasker.model.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mk.gpstasker.model.INTERNET_AVAILABLE
import com.mk.gpstasker.model.INTERNET_UNAVAILABLE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

//ext function
fun Context.checkInternet():Boolean
{
        val activeNetwork = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork
        return (activeNetwork != null)
}

class NetworkUtil(private val connectivityManager: ConnectivityManager) {
    private val _internetAvailable = MutableLiveData(true)
    val internetAvailable : LiveData<Boolean>
    get() = _internetAvailable


    private val netCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            //TODO:REMOVE
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            updateNetworkStatus(INTERNET_UNAVAILABLE)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            updateNetworkStatus(INTERNET_UNAVAILABLE)


        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            CoroutineScope(Dispatchers.IO).launch {
                updateNetworkStatus(hasInternet(network))
            }
        }
    }



    fun registerNetworkCallback(){
        val networkRequest = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        connectivityManager.registerNetworkCallback(networkRequest,netCallback)
    }

    fun unregisterNetworkCallback(){
        connectivityManager.unregisterNetworkCallback(netCallback)
    }


    /**
     * updates only if the state actually changed
     * */
    private fun updateNetworkStatus(newStatus:Boolean){
        if(internetAvailable.value != newStatus)
            _internetAvailable.postValue(newStatus)
    }


        private fun hasInternet(network: Network): Boolean {
            return try {
                network.socketFactory.createSocket()?.let {
                    it.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                    it.close()
                    return true
                } ?: false

            } catch (e: Exception) {
                false
            }

        }
}