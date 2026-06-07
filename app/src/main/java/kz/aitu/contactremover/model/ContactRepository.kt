package kz.aitu.contactremover.model

data class PhoneContact(
    val name: String,
    val phone: String,
    val carrier: String = ""
)

object ContactRepository {
    
    val presetContacts: List<PhoneContact> = buildList {
        // АЛТЕЛ серия 700-000-XXXX (100 контактов)
        for (i in 0..99) {
            val suffix = i.toString().padStart(4, '0')
            val phone = "+7700${suffix}6982"
            val formatted = "+7 700 ${suffix.substring(0,2)}${suffix.substring(2)} 69-82"
            add(PhoneContact(
                name = "АЛТЕЛ ${i + 1}",
                phone = phone,
                carrier = "АЛТЕЛ"
            ))
        }
        
        // Дополнительные серии
        val extraSeries = listOf(
            Triple("АЛТЕЛ GSM", "+7700", listOf("0016982","0026982","0036982","0046982","0056982")),
            Triple("Beeline", "+7705", listOf("1234567","2345678","3456789","4567890","5678901")),
            Triple("Kcell", "+7702", listOf("9876543","8765432","7654321","6543210","5432109")),
            Triple("Tele2", "+7707", listOf("1111111","2222222","3333333","4444444","5555555"))
        )
        
        for ((carrier, prefix, numbers) in extraSeries) {
            for (num in numbers) {
                add(PhoneContact(
                    name = "$carrier $num",
                    phone = "$prefix$num",
                    carrier = carrier
                ))
            }
        }
    }
    
    fun search(query: String): List<PhoneContact> {
        if (query.isBlank()) return presetContacts
        val q = query.lowercase().trim()
        return presetContacts.filter {
            it.name.lowercase().contains(q) ||
            it.phone.contains(q) ||
            it.carrier.lowercase().contains(q)
        }
    }
}
