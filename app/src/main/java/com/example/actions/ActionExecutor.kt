package com.example.actions

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import com.example.commands.CommandAction
import com.example.commands.ParsedCommand
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActionExecutor(private val context: Context) {

    private var isTorchOn = false

    fun execute(command: ParsedCommand): String {
        return try {
            when (command.action) {
                CommandAction.FLASHLIGHT_ON -> setFlashlight(true)
                CommandAction.FLASHLIGHT_OFF -> setFlashlight(false)
                CommandAction.OPEN_CAMERA -> openCamera()
                CommandAction.OPEN_YOUTUBE -> openYoutube(null)
                CommandAction.SEARCH_YOUTUBE -> openYoutube(command.parameters["query"])
                CommandAction.CALL_NUMBER -> callNumber(command.parameters["phone_number"] ?: "")
                CommandAction.CALL_CONTACT -> callContact(command.parameters["contact_name"] ?: "")
                CommandAction.SEND_SMS -> sendSms(
                    command.parameters["phone_number"] ?: command.parameters["contact_name"] ?: "",
                    command.parameters["message"] ?: ""
                )
                CommandAction.OPEN_WHATSAPP, CommandAction.OPEN_WHATSAPP_CHAT -> openWhatsapp(
                    command.parameters["contact_name"] ?: command.parameters["phone_number"],
                    command.parameters["message"]
                )
                CommandAction.OPEN_SETTINGS -> openSettings(Settings.ACTION_SETTINGS)
                CommandAction.OPEN_WIFI_SETTINGS -> openSettings(Settings.ACTION_WIFI_SETTINGS)
                CommandAction.OPEN_BLUETOOTH_SETTINGS -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
                CommandAction.OPEN_MOBILE_NETWORK_SETTINGS -> openSettings(Settings.ACTION_DATA_ROAMING_SETTINGS)
                CommandAction.OPEN_CONTACTS -> openContacts()
                CommandAction.OPEN_GALLERY -> openGallery()
                CommandAction.SET_ALARM -> setAlarm(command.parameters["time"])
                CommandAction.CREATE_REMINDER -> createReminder(command.parameters["title"] ?: "میٹنگ / کام")
                CommandAction.GET_TIME -> getCurrentUrduTime()
                CommandAction.GET_WEATHER -> "آج موسم عام طور پر صاف اور خوشگوار رہنے کی توقع ہے۔"
                CommandAction.GENERAL_RESPONSE, CommandAction.UNKNOWN -> command.spokenResponse
            }
        } catch (e: Exception) {
            "معذرت، اس کام کی ادائیگی میں مسئلہ ہوا: ${e.localizedMessage}"
        }
    }

    private fun setFlashlight(turnOn: Boolean): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, turnOn)
                isTorchOn = turnOn
                if (turnOn) "فلیش لائٹ آن کر دی گئی ہے۔" else "فلیش لائٹ بند کر دی گئی ہے۔"
            } else {
                "اس ڈیوائس میں فلیش لائٹ دستیاب نہیں ہے۔"
            }
        } catch (e: Exception) {
            "فلیش لائٹ کنٹرول کرنے کی اجازت یا سہولت دستیاب نہیں ہے۔"
        }
    }

    private fun openCamera(): String {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            "کیمرا کھول دیا گیا ہے۔"
        } else {
            "کیمرا ایپ نہیں مل سکی۔"
        }
    }

    private fun openYoutube(query: String?): String {
        return if (!query.isNullOrBlank()) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:results?search_query=$encodedQuery")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (appIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appIntent)
                "یوٹیوب پر $query تلاش کیا جا رہا ہے۔"
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                "برائوزر میں یوٹیوب پر $query تلاش کیا جا رہا ہے۔"
            }
        } else {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                "یوٹیوب کھول دی گئی ہے۔"
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                "یوٹیوب برائوزر میں کھولی جا رہی ہے۔"
            }
        }
    }

    private fun callNumber(number: String): String {
        val cleanNumber = number.filter { it.isDigit() || it == '+' }
        if (cleanNumber.isBlank()) return "کال کرنے کے لیے درست نمبر فراہم نہیں کیا گیا۔"

        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(dialIntent)
        return "$cleanNumber ڈائلر میں بھیج دیا گیا ہے۔"
    }

    private fun callContact(contactName: String): String {
        val foundNumber = searchContactPhoneNumber(contactName)
        return if (foundNumber != null) {
            callNumber(foundNumber)
        } else {
            val contactsIntent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(contactsIntent)
            "رابطوں میں $contactName نہیں ملا، لہذا تمام contacts کھول دیے گئے ہیں۔"
        }
    }

    private fun searchContactPhoneNumber(name: String): String? {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex >= 0) return it.getString(numberIndex)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun sendSms(target: String, message: String): String {
        val number = if (target.any { it.isDigit() }) target else (searchContactPhoneNumber(target) ?: target)
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$number")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(smsIntent)
        return "پیغام SMS میں تیار کر دیا گیا ہے۔"
    }

    private fun openWhatsapp(target: String?, message: String?): String {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (!message.isNullOrBlank()) {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                        setPackage("com.whatsapp")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(sendIntent)
                    "واٹس ایپ پر پیغام تیار کر دیا گیا ہے۔"
                } else {
                    context.startActivity(launchIntent)
                    "واٹس ایپ کھول دی گئی ہے۔"
                }
            } else {
                "واٹس ایپ اس موبائل میں انسٹال نہیں ہے۔"
            }
        } catch (e: Exception) {
            "واٹس ایپ کھولنے میں ناکامی ہوئی: ${e.localizedMessage}"
        }
    }

    private fun openSettings(actionStr: String): String {
        val intent = Intent(actionStr).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "مطلوبہ سیٹنگز کھول دی گئی ہیں۔"
    }

    private fun openContacts(): String {
        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "فون کے contacts کھول دیے گئے ہیں۔"
    }

    private fun openGallery(): String {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return "گیلری کھول دی گئی ہے۔"
    }

    private fun setAlarm(timeStr: String?): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, "Munwar AI Alarm")
            putExtra(AlarmClock.EXTRA_HOUR, 7)
            putExtra(AlarmClock.EXTRA_MINUTES, 0)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            "الارم کی ایپ کھول دی گئی ہے۔"
        } else {
            openSettings(Settings.ACTION_DATE_SETTINGS)
        }
    }

    private fun createReminder(title: String): String {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = Uri.parse("content://com.android.calendar/events")
            putExtra("title", title)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            "کیلنڈر میں یاد دہانی تیار کر دی گئی ہے۔"
        } else {
            "کیلنڈر ایپ نہیں مل سکی۔"
        }
    }

    private fun getCurrentUrduTime(): String {
        val sdfTime = SimpleDateFormat("hh:mm", Locale.US)
        val sdfAmpm = SimpleDateFormat("a", Locale.US)
        val now = Date()
        val timeFormatted = sdfTime.format(now)
        val ampm = if (sdfAmpm.format(now).equals("AM", ignoreCase = true)) "صبح" else "شام"
        return "اس وقت $ampm $timeFormatted بج رہے ہیں۔"
    }
}
