package com.stapterminal.nfc

/**
 * Terminal-side (reader) definition of the STap payment protocol: a custom AID plus a
 * handful of proprietary APDU instructions layered on top of standard ISO 7816-4 framing
 * (CLA INS P1 P2 Lc <data>).
 *
 * This app drives the exchange over [android.nfc.tech.IsoDep]; the STap wallet only ever
 * responds. Must stay in sync with STap's copy of this object.
 */
object StapNfcProtocol {

    /** Proprietary AID identifying the STap card-emulation application. */
    val AID: ByteArray = byteArrayOf(
        0xF0.toByte(), 0x53, 0x54, 0x41, 0x50, 0x01, 0x00,
    )

    /** Terminal -> STap: one chunk of the unsigned transaction message to sign. */
    const val INS_PUT_MESSAGE_CHUNK: Byte = 0xC1.toByte()

    /** Terminal -> STap: final payment outcome, after the transaction has been submitted. */
    const val INS_STATUS: Byte = 0xC2.toByte()

    /** P2 on [INS_PUT_MESSAGE_CHUNK]: more chunks are coming. */
    const val P2_MORE_CHUNKS: Byte = 0x00
    /** P2 on [INS_PUT_MESSAGE_CHUNK]: this is the last chunk; sign the assembled message. */
    const val P2_LAST_CHUNK: Byte = 0x01

    /** P1 on [INS_STATUS]. */
    const val STATUS_SUCCESS: Byte = 0x01
    const val STATUS_FAILURE: Byte = 0x00

    /** Conservative chunk size, safely under the 255-byte short-form APDU Lc limit. */
    const val DEFAULT_CHUNK_SIZE = 200
    const val STATUS_MESSAGE_MAX_BYTES = 200

    fun buildSelectApdu(): ByteArray =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, AID.size.toByte()) + AID + byteArrayOf(0x00)

    fun buildChunkApdu(seq: Int, isLast: Boolean, data: ByteArray): ByteArray =
        byteArrayOf(
            0x00,
            INS_PUT_MESSAGE_CHUNK,
            seq.toByte(),
            if (isLast) P2_LAST_CHUNK else P2_MORE_CHUNKS,
            data.size.toByte(),
        ) + data

    fun buildStatusApdu(success: Boolean, message: ByteArray): ByteArray =
        byteArrayOf(
            0x00,
            INS_STATUS,
            if (success) STATUS_SUCCESS else STATUS_FAILURE,
            0x00,
            message.size.toByte(),
        ) + message
}

/** True if this response APDU ends with the ISO 7816-4 "success" status word (90 00). */
fun ByteArray.isSuccessSw(): Boolean =
    size >= 2 && this[size - 2] == 0x90.toByte() && this[size - 1] == 0x00.toByte()

/** The data portion of a response APDU, i.e. everything before the trailing status word. */
fun ByteArray.responseData(): ByteArray =
    if (size >= 2) copyOfRange(0, size - 2) else ByteArray(0)
