@file:Suppress("DEPRECATION")
package com.sameerasw.essentials.utils

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

object AdbDiscoveryHolder {
    private var discovery: AdbMdnsDiscovery? = null

    fun initialize(context: Context) {
        if (discovery == null) {
            Log.d("AdbDiscoveryHolder", "Initializing ADB discovery")
            discovery = AdbMdnsDiscovery(context.applicationContext)
            discovery?.startDiscovery()
        }
    }

    fun restartDiscovery(context: Context) {
        Log.d("AdbDiscoveryHolder", "Restarting ADB discovery")
        discovery?.stopDiscovery()
        discovery = null
        initialize(context)
    }

    fun isDiscoveryActive(): Boolean {
        return discovery != null
    }

    fun getDiscoveredServices(): List<AdbMdnsDiscovery.AdbServiceInfo> {
        return discovery?.getDiscoveredServices() ?: emptyList()
    }
}

class AdbMdnsDiscovery(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val TAG = "AdbMdnsDiscovery"
    private val discoveredServices = mutableListOf<AdbServiceInfo>()
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isDiscovering = false

    data class AdbServiceInfo(
        val serviceName: String,
        val hostAddress: String,
        val port: Int
    )

    private fun createDiscoveryListener(): NsdManager.DiscoveryListener {
        return object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "mDNS Discovery Started for service type: $regType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")

                synchronized(discoveredServices) {
                    if (discoveredServices.any { it.serviceName == serviceInfo.serviceName }) {
                        return
                    }
                }

                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: Error code $errorCode")
                    }

                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                        val host = resolvedInfo.host
                        val port = resolvedInfo.port
                        val hostAddress = host?.hostAddress ?: "unknown"

                        synchronized(discoveredServices) {
                            if (discoveredServices.none { it.serviceName == resolvedInfo.serviceName }) {
                                discoveredServices.add(
                                    AdbServiceInfo(
                                        serviceName = resolvedInfo.serviceName,
                                        hostAddress = hostAddress,
                                        port = port
                                    )
                                )
                            }
                        }
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                synchronized(discoveredServices) {
                    discoveredServices.removeAll { it.serviceName == serviceInfo.serviceName }
                }
            }

            override fun onDiscoveryStopped(regType: String) {
                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(regType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(regType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
        }
    }

    fun startDiscovery() {
        if (isDiscovering) return
        try {
            discoveredServices.clear()
            discoveryListener = createDiscoveryListener()
            nsdManager.discoverServices(
                "_adb-tls-connect._tcp.",
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener!!
            )
            isDiscovering = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
        }
    }

    fun stopDiscovery() {
        if (!isDiscovering || discoveryListener == null) return
        try {
            nsdManager.stopServiceDiscovery(discoveryListener!!)
            isDiscovering = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop discovery", e)
        }
    }

    fun getDiscoveredServices(): List<AdbServiceInfo> {
        synchronized(discoveredServices) {
            return discoveredServices.toList()
        }
    }
}
