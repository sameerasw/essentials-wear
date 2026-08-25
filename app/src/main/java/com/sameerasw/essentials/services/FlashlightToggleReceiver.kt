package com.sameerasw.essentials.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.utils.HapticUtil

class FlashlightToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        HapticUtil.performClick(context)
        try {
            val nodeClient = Wearable.getNodeClient(context)
            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                val messageClient = Wearable.getMessageClient(context)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/toggle_flashlight", byteArrayOf())
                }
            }
        } catch (_: Exception) { }
    }
}
