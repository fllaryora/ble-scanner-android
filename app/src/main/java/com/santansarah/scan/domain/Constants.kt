package com.santansarah.scan.domain

import com.santansarah.scan.domain.models.UUID_DEFAULT

const val YOUTUBE_LINK = "https://www.youtube.com/@santansarah"
const val LINKEDIN_LINK = "https://www.linkedin.com/in/santansarah"
const val GITHUB_LINK = "https://www.github.com/santansarah"
const val DISCUSSIONS_LINK = "https://github.com/santansarah/ble-scanner/discussions"
const val BUG_LINK = "https://github.com/santansarah/ble-scanner/issues"
const val ABOUT_LINK = "https://github.com/santansarah/ble-scanner"
const val PRIVACY_POLICY = "https://github.com/santansarah/ble-scanner/blob/main/PrivacyPolicy.md"
const val TERMS_LINK = "https://github.com/santansarah/ble-scanner/blob/main/Terms.md"

const val MICROSOFT = 6

enum class Chemistry(val nameCh: String, val nameService: String,
                     val serviceUUIIID: String,
                     val characteristicUUID: String, val descriptorUUID: String) {
    CARBON_MONOXIDE("Carbon monoxide", "First",
        "43dc60ce-42a1-11ee-be56-0242ac120001",
        "43dc60ce-42b1-11ee-be56-0242ac120001",
        "00003901$UUID_DEFAULT".lowercase()
    ),
    CARBON_DIOXIDE("Carbon dixide","First",
        "43dc60ce-42a1-11ee-be56-0242ac120001",
        "43dc60ce-42b2-11ee-be56-0242ac120001",
        "00003902$UUID_DEFAULT".lowercase()
    ),
    ALCOHOL("Alcohol", "Second",
        "43dc60ce-42a2-11ee-be56-0242ac120001",
        "43dc60ce-42b3-11ee-be56-0242ac120001",
        "00003903$UUID_DEFAULT".lowercase()
    ),
    ACETONE("Acetone", "Second",
        "43dc60ce-42a2-11ee-be56-0242ac120001",
        "43dc60ce-42b4-11ee-be56-0242ac120001",
        "00003904$UUID_DEFAULT".lowercase()
    ),
    TOLUENE("Toluene", "Thirst",
        "43dc60ce-42a3-11ee-be56-0242ac120001",
        "43dc60ce-42b5-11ee-be56-0242ac120001",
        "00003905$UUID_DEFAULT".lowercase()
    ),
    AMMONIUM("Ammonium","Thirst",
        "43dc60ce-42a3-11ee-be56-0242ac120001",
        "43dc60ce-42b6-11ee-be56-0242ac120001",
        "00003906$UUID_DEFAULT".lowercase()
    ),
    PERCENTAGE("Percentage", "Percentage",
        "43dc60ce-42a4-11ee-be56-0242ac120001",
        "43dc60ce-42b7-11ee-be56-0242ac120001",
        "00003907$UUID_DEFAULT".lowercase()
    );

    companion object {
        fun getNameByServiceUUID(serviceUUI:String): String {
            values().forEach { enumerated : Chemistry ->
                if(enumerated.serviceUUIIID == serviceUUI)
                    return enumerated.nameService
            }
            return "Manufactured Service"
        }

        fun getNameByCharacteristicUUID(characteristicUUID:String): String {
            values().forEach { enumerated : Chemistry ->
                if(enumerated.characteristicUUID == characteristicUUID)
                    return enumerated.nameCh
            }
            return "Manufactured Characteristic"
        }

        fun getNameByDescriptorUUID(descriptorUUID:String): String {
            values().forEach { enumerated : Chemistry ->
                if(enumerated.descriptorUUID == descriptorUUID)
                    return enumerated.nameCh
            }
            return "Manufactured Descriptor"
        }
    }
}
