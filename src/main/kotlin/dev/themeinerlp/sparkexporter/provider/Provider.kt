package dev.themeinerlp.sparkexporter.provider

import com.influxdb.client.WriteApi

interface Provider {
    fun writeData(writeApiBlocking: WriteApi)
}