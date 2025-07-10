package com.example.flipperdroid.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import java.io.IOException

object MifareClassicUtils {
    const val NO_DATA = "--------------------------------"
    const val DEFAULT_KEY = "FFFFFFFFFFFF"

    fun bytesToHex(bytes: ByteArray?): String {
        if (bytes == null) return ""
        return bytes.joinToString("") { "%02X".format(it) }
    }

    fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return try {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            Log.d("Debug","Error to convert hex to byte: ${e.message}")
            null
        }
    }


    fun readSector(tag: Tag, sectorIndex: Int, key: ByteArray, useAsKeyB: Boolean): List<String>? {
        val mfc = MifareClassic.get(tag) ?: return null
        try {
            mfc.connect()
            val auth = if (useAsKeyB) mfc.authenticateSectorWithKeyB(sectorIndex, key)
            else mfc.authenticateSectorWithKeyA(sectorIndex, key)
            if (!auth) return null
            val firstBlock = mfc.sectorToBlock(sectorIndex)
            val blockCount = mfc.getBlockCountInSector(sectorIndex)
            val blocks = mutableListOf<String>()
            for (i in 0 until blockCount) {
                try {
                    val blockBytes = mfc.readBlock(firstBlock + i)
                    blocks.add(bytesToHex(blockBytes))
                } catch (e: IOException) {
                    blocks.add(NO_DATA)
                }
            }
            return blocks
        } catch (e: Exception) {
            return null
        } finally {
            try { mfc.close() } catch (_: Exception) {}
        }
    }

    fun writeBlock(tag: Tag, sectorIndex: Int, blockIndex: Int, data: ByteArray, key: ByteArray, useAsKeyB: Boolean): Boolean {
        val mfc = MifareClassic.get(tag) ?: return false
        try {
            mfc.connect()
            val auth = if (useAsKeyB) mfc.authenticateSectorWithKeyB(sectorIndex, key)
            else mfc.authenticateSectorWithKeyA(sectorIndex, key)
            if (!auth) return false
            val block = mfc.sectorToBlock(sectorIndex) + blockIndex
            mfc.writeBlock(block, data)
            return true
        } catch (e: Exception) {
            return false
        } finally {
            try { mfc.close() } catch (_: Exception) {}
        }
    }

    fun cloneUid(tag: Tag, newUid: ByteArray): Boolean {
        // ATTENTION: Cette opération ne fonctionne que sur les cartes "magic" (génériques) !
        // On tente d'écrire le block 0 (UID) avec la nouvelle valeur
        val mfc = MifareClassic.get(tag) ?: return false
        try {
            mfc.connect()
            // Authentification avec le key A par défaut
            val auth = mfc.authenticateSectorWithKeyA(0, hexToBytes(DEFAULT_KEY) ?: return false)
            if (!auth) return false
            val block0 = mfc.readBlock(0)
            val newBlock0 = newUid + block0.copyOfRange(newUid.size, block0.size)
            mfc.writeBlock(0, newBlock0)
            return true
        } catch (e: Exception) {
            return false
        } finally {
            try { mfc.close() } catch (_: Exception) {}
        }
    }
} 