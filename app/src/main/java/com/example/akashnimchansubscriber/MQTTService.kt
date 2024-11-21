package com.example.akashnimchansubscriber
import android.content.Context
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class MQTTService(
    context: Context,
    private val locationUpdateListener: LocationListener
) {

    companion object {
        private const val MQTT_BROKER_HOST = "broker.sundaebytestt.com"
        private const val MQTT_BROKER_PORT = 1883
        private const val MQTT_CLIENT_ID = "client"
        private const val LOCATION_TOPIC = "assignment/location"

        // JSON Keys
        private const val JSON_KEY_LATITUDE = "latitude"
        private const val JSON_KEY_LONGITUDE = "longitude"
        private const val JSON_KEY_ID = "id"
        private const val JSON_KEY_TIMESTAMP = "timestamp"
        private const val JSON_KEY_SPEED = "speed"
    }

    private val mqttClient: Mqtt5AsyncClient = MqttClient.builder()
        .useMqttVersion5()
        .identifier(MQTT_CLIENT_ID)
        .serverHost(MQTT_BROKER_HOST)
        .serverPort(MQTT_BROKER_PORT)
        .buildAsync()

    private val databaseHelper = DatabaseHelper(context, null)

    init {
        connectToBroker()
    }

    private fun connectToBroker() {
        mqttClient.connect().whenComplete { _, error ->
            if (error != null) {
                logError("Failed to connect to MQTT broker", error)
            } else {
                println("Successfully connected to MQTT broker")
                subscribeToLocationTopic()
            }
        }
    }

    private fun subscribeToLocationTopic() {
        mqttClient.subscribeWith()
            .topicFilter(LOCATION_TOPIC)
            .callback { publish -> processIncomingMessage(publish) }
            .send()
            .whenComplete { _, error ->
                if (error != null) {
                    logError("Failed to subscribe to topic: $LOCATION_TOPIC", error)
                } else {
                    println("Successfully subscribed to topic: $LOCATION_TOPIC")
                }
            }
    }

    private fun processIncomingMessage(publish: Mqtt5Publish) {
        try {
            val message = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
            val jsonData = parseMessageToJson(message)
            saveLocationDataToDatabase(jsonData)
            notifyLocationUpdateListener(jsonData.getString(JSON_KEY_ID))
        } catch (e: Exception) {
            logError("Error while processing incoming MQTT message", e)
        }
    }

    private fun parseMessageToJson(message: String): JSONObject {
        return JSONObject(message)
    }

    private fun saveLocationDataToDatabase(json: JSONObject) {
        databaseHelper.createLocation(
            latitude = json.getDouble(JSON_KEY_LATITUDE),
            longitude = json.getDouble(JSON_KEY_LONGITUDE),
            id = json.getString(JSON_KEY_ID),
            timestamp = json.getInt(JSON_KEY_TIMESTAMP),
            speed = json.getDouble(JSON_KEY_SPEED)
        )
    }

    private fun notifyLocationUpdateListener(id: String) {
        locationUpdateListener.onNewLocationReceived(id)
    }

    private fun logError(message: String, throwable: Throwable) {
        println("$message: ${throwable.message}")
        throwable.printStackTrace()
    }
}
