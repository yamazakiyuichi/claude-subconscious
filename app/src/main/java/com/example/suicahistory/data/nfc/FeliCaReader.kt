package com.example.suicahistory.data.nfc

import android.nfc.Tag
import android.nfc.tech.NfcF
import com.example.suicahistory.data.db.Transaction
import com.example.suicahistory.domain.CategoryClassifier
import java.nio.ByteBuffer
import java.util.Calendar

/**
 * Suica FeliCa履歴ブロック読み取り
 * System code: 0x0003, Service code: 0x090F（利用履歴）
 */
object FeliCaReader {

    private const val SYSTEM_CODE_SUICA = 0x0003
    private const val SERVICE_CODE_HISTORY = 0x090F
    private const val BLOCK_COUNT = 20

    fun readHistory(tag: Tag): List<Transaction> {
        val nfcF = NfcF.get(tag) ?: return emptyList()
        return try {
            nfcF.connect()
            val idm = nfcF.tag.id
            val blocks = readBlocks(nfcF, idm)
            blocks.mapNotNull { parseBlock(it) }
        } catch (e: Exception) {
            emptyList()
        } finally {
            runCatching { nfcF.close() }
        }
    }

    private fun readBlocks(nfcF: NfcF, idm: ByteArray): List<ByteArray> {
        val results = mutableListOf<ByteArray>()
        // Read 4 blocks at a time
        for (start in 0 until BLOCK_COUNT step 4) {
            val count = minOf(4, BLOCK_COUNT - start)
            val cmd = buildReadCommand(idm, SERVICE_CODE_HISTORY, start, count)
            val response = nfcF.transceive(cmd)
            if (response.size < 13) break
            val blockCount = response[9].toInt() and 0xFF
            for (i in 0 until blockCount) {
                val offset = 10 + i * 16
                if (offset + 16 <= response.size) {
                    results.add(response.copyOfRange(offset, offset + 16))
                }
            }
        }
        return results
    }

    private fun buildReadCommand(idm: ByteArray, serviceCode: Int, startBlock: Int, count: Int): ByteArray {
        val blockList = (startBlock until startBlock + count).map { block ->
            byteArrayOf(0x80.toByte(), block.toByte())
        }
        val totalLen = 14 + count * 2
        val cmd = ByteArray(totalLen)
        cmd[0] = totalLen.toByte()
        cmd[1] = 0x06  // Read Without Encryption
        System.arraycopy(idm, 0, cmd, 2, 8)
        cmd[10] = 0x01  // 1 service
        cmd[11] = (serviceCode and 0xFF).toByte()
        cmd[12] = ((serviceCode shr 8) and 0xFF).toByte()
        cmd[13] = count.toByte()
        blockList.forEachIndexed { i, b ->
            System.arraycopy(b, 0, cmd, 14 + i * 2, 2)
        }
        return cmd
    }

    private fun parseBlock(block: ByteArray): Transaction? {
        if (block.size < 16) return null
        // Suica history block format:
        // [0] console type, [1] process type
        // [2-3] date (YY/MM/DD packed in BCD-like)
        // [4-5] enter station, [6-7] exit station
        // [8-9] balance (little-endian)
        // [10-15] more fields

        val consoleType = block[0].toInt() and 0xFF
        val processType = block[1].toInt() and 0xFF

        // Skip empty blocks
        if (consoleType == 0 && processType == 0) return null

        val dateRaw = ByteBuffer.wrap(byteArrayOf(0, 0, block[2], block[3])).int
        val year = 2000 + ((dateRaw shr 9) and 0x7F)
        val month = (dateRaw shr 5) and 0x0F
        val day = dateRaw and 0x1F
        if (month == 0 || day == 0) return null

        val cal = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val balance = ((block[11].toInt() and 0xFF) or ((block[10].toInt() and 0xFF) shl 8))
        val shopName = resolveShopName(consoleType, processType, block)
        val amount = resolveAmount(processType, block)

        return Transaction(
            date = cal.timeInMillis,
            shopName = shopName,
            amount = amount,
            balance = balance,
            category = CategoryClassifier.classify(shopName),
            source = "nfc"
        )
    }

    private fun resolveShopName(consoleType: Int, processType: Int, block: ByteArray): String {
        // Console type: 3=物販, 16=自動改札, 22=バス, etc.
        return when (consoleType) {
            0x03 -> "物販 (NFC)"
            0x08, 0x12, 0x13 -> {
                val enter = ((block[4].toInt() and 0xFF) shl 8) or (block[5].toInt() and 0xFF)
                val exit = ((block[6].toInt() and 0xFF) shl 8) or (block[7].toInt() and 0xFF)
                "入場 [$enter] → 出場 [$exit]"
            }
            0x16 -> "バス乗車"
            else -> "利用 (type=0x${consoleType.toString(16)})"
        }
    }

    private fun resolveAmount(processType: Int, block: ByteArray): Int {
        // Process type 0x46=物販, 0x0F=チャージ
        val raw = ((block[8].toInt() and 0xFF) shl 8) or (block[9].toInt() and 0xFF)
        return if (processType == 0x0F) raw else -raw
    }
}
