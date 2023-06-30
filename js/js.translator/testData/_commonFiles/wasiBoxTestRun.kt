@kotlin.wasm.WasmExport
fun runBoxTest(): Boolean {
    val boxResult = box()
    val isOk = boxResult == "OK"
    if (!isOk) {
        println("Wrong box result '${boxResult}'; Expected 'OK'")
    }
    return isOk
}