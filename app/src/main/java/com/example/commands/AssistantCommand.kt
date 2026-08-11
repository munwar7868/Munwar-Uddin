package com.example.commands

import org.json.JSONObject

enum class CommandAction {
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    OPEN_CAMERA,
    OPEN_YOUTUBE,
    SEARCH_YOUTUBE,
    CALL_NUMBER,
    CALL_CONTACT,
    SEND_SMS,
    OPEN_WHATSAPP,
    OPEN_WHATSAPP_CHAT,
    OPEN_SETTINGS,
    OPEN_WIFI_SETTINGS,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_MOBILE_NETWORK_SETTINGS,
    OPEN_CONTACTS,
    OPEN_GALLERY,
    SET_ALARM,
    CREATE_REMINDER,
    GET_WEATHER,
    GET_TIME,
    GENERAL_RESPONSE,
    UNKNOWN
}

data class ParsedCommand(
    val action: CommandAction,
    val parameters: Map<String, String> = emptyMap(),
    val confirmationRequired: Boolean = false,
    val spokenResponse: String
) {
    companion object {
        fun fromJson(jsonStr: String): ParsedCommand {
            return try {
                val cleanedJson = jsonStr.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val json = JSONObject(cleanedJson)
                val actionStr = json.optString("action", "UNKNOWN").uppercase()
                val action = try {
                    CommandAction.valueOf(actionStr)
                } catch (e: Exception) {
                    CommandAction.UNKNOWN
                }

                val paramsMap = mutableMapOf<String, String>()
                val paramsObj = json.optJSONObject("parameters")
                if (paramsObj != null) {
                    val keys = paramsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        paramsMap[key] = paramsObj.optString(key, "")
                    }
                }

                val confirmation = json.optBoolean("confirmation_required", false)
                val spoken = json.optString("spoken_response", "جی، فرمائیے۔")

                ParsedCommand(
                    action = action,
                    parameters = paramsMap,
                    confirmationRequired = confirmation,
                    spokenResponse = spoken
                )
            } catch (e: Exception) {
                ParsedCommand(
                    action = CommandAction.UNKNOWN,
                    parameters = emptyMap(),
                    confirmationRequired = false,
                    spokenResponse = "معذرت، میں آپ کا حکم مکمل طور پر سمجھ نہیں سکی۔"
                )
            }
        }
    }
}
