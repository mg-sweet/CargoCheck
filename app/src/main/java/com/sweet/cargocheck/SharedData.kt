package com.sweet.cargocheck

object SharedData {
    var cargoCsvData: List<List<String>> = emptyList()
    var obCsvData: List<List<String>> = emptyList()

    var selectedCargoWaybillIndex: Int = -1
    var selectedCargoFilterIndex: Int = -1
    var selectedObWaybillIndex: Int = -1

    // TextView ပေါ်ရှိ Filter လုပ်ပြီးသား စာသားများကို သိမ်းရန်
    var filteredCargoText: String = ""
    var filteredObText: String = ""

    // for IB
    var deliveryCsvData: List<List<String>> = emptyList() // IB အတွက်
    var pendingCsvData: List<List<String>> = emptyList()  // IB အတွက်

    // IB အတွက် Index များ
    var selectedDeliveryIndex = -1
    var selectedPendingIndex = -1

    // IB အတွက် Filtered Text များ
    var filteredDeliveryText = ""
    var filteredPendingText = ""
}