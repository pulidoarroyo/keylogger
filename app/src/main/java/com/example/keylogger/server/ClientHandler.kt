package com.example.keylogger.server

import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

class ClientHandler(val clientSocket: Socket, private val onClientSocketClose: (socket: ClientHandler) -> Unit) : Thread() {
    override fun run() {
        while (!clientSocket.isClosed && clientSocket.isBound) {
            val dataInputStream = DataInputStream(clientSocket.getInputStream())
            try {
                if(dataInputStream.available() > 0){
                    val data = dataInputStream.readUTF()
                    clientSocket.close()
                    onClientSocketClose(this)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Log.d("ClientHandler", "Finalizando hilo")
    }

    fun writeMessage(message: String){
        try{
            val dataOutputStream = DataOutputStream(clientSocket.getOutputStream())
            dataOutputStream.writeUTF(message)
        }catch (e: Exception) {
            clientSocket.close()
            onClientSocketClose(this)
        }
    }

}