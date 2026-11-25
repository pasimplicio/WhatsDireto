package br.com.whatsdireto.domain

/**
 * Pure Kotlin Multiplatform QR Code encoder (ISO/IEC 18004, Byte Mode, ECC Level M, Versions 1..10).
 * Supports UTF-8 payloads up to 213 bytes — more than enough for wa.me links with messages.
 */
class QrCodeMatrix(
    val size: Int,
    private val modules: BooleanArray
) {
    operator fun get(x: Int, y: Int): Boolean = modules[y * size + x]

    companion object {
        // Version 1..10 parameters for Error Correction Level M:
        // (totalCodewords, ecCodewordsPerBlock, numBlocksGroup1, dataCodewordsGroup1, numBlocksGroup2, dataCodewordsGroup2)
        private data class VersionSpec(
            val version: Int,
            val totalCodewords: Int,
            val ecPerBlock: Int,
            val g1Blocks: Int,
            val g1Data: Int,
            val g2Blocks: Int = 0,
            val g2Data: Int = 0,
            val alignmentCenters: IntArray = intArrayOf()
        ) {
            val dataCapacityBytes: Int get() = g1Blocks * g1Data + g2Blocks * g2Data
        }

        private val SPECS = listOf(
            VersionSpec(1, 26, 10, 1, 16, alignmentCenters = intArrayOf()),
            VersionSpec(2, 44, 16, 1, 28, alignmentCenters = intArrayOf(6, 18)),
            VersionSpec(3, 70, 26, 1, 44, alignmentCenters = intArrayOf(6, 22)),
            VersionSpec(4, 100, 18, 2, 32, alignmentCenters = intArrayOf(6, 26)),
            VersionSpec(5, 134, 24, 2, 43, alignmentCenters = intArrayOf(6, 30)),
            VersionSpec(6, 172, 16, 4, 27, alignmentCenters = intArrayOf(6, 34)),
            VersionSpec(7, 196, 18, 4, 31, alignmentCenters = intArrayOf(6, 22, 38)),
            VersionSpec(8, 242, 22, 2, 38, 2, 39, alignmentCenters = intArrayOf(6, 24, 42)),
            VersionSpec(9, 292, 22, 3, 36, 2, 37, alignmentCenters = intArrayOf(6, 26, 46)),
            VersionSpec(10, 346, 26, 4, 43, 1, 44, alignmentCenters = intArrayOf(6, 28, 50))
        )

        private val GF_EXP = IntArray(512)
        private val GF_LOG = IntArray(256)

        init {
            var x = 1
            for (i in 0 until 255) {
                GF_EXP[i] = x
                GF_LOG[x] = i
                x = x shl 1
                if (x >= 256) x = x xor 0x11D
            }
            for (i in 255 until 512) {
                GF_EXP[i] = GF_EXP[i - 255]
            }
        }

        fun encode(text: String): QrCodeMatrix? {
            val payload = text.encodeToByteArray()
            val spec = SPECS.firstOrNull { candidate ->
                val countBits = if (candidate.version <= 9) 8 else 16
                val requiredBits = 4 + countBits + payload.size * 8
                requiredBits <= candidate.dataCapacityBytes * 8
            } ?: return null

            val dataCodewords = buildDataCodewords(payload, spec)
            val allCodewords = interleaveWithEcc(dataCodewords, spec)
            return renderBestMask(allCodewords, spec)
        }

        private fun buildDataCodewords(payload: ByteArray, spec: VersionSpec): IntArray {
            val bits = mutableListOf<Int>()
            fun appendBits(value: Int, length: Int) {
                for (i in (length - 1) downTo 0) {
                    bits.add((value ushr i) and 1)
                }
            }

            // Byte mode indicator (0100)
            appendBits(0b0100, 4)
            val countBits = if (spec.version <= 9) 8 else 16
            appendBits(payload.size, countBits)
            for (b in payload) {
                appendBits(b.toInt() and 0xFF, 8)
            }

            val capacityBits = spec.dataCapacityBytes * 8
            val terminator = minOf(4, capacityBits - bits.size)
            appendBits(0, terminator)
            while (bits.size % 8 != 0) bits.add(0)

            val padBytes = intArrayOf(0xEC, 0x11)
            var padIndex = 0
            while (bits.size < capacityBits) {
                appendBits(padBytes[padIndex % 2], 8)
                padIndex++
            }

            return IntArray(spec.dataCapacityBytes) { byteIdx ->
                var acc = 0
                for (bit in 0 until 8) {
                    acc = (acc shl 1) or bits[byteIdx * 8 + bit]
                }
                acc
            }
        }

        private fun interleaveWithEcc(data: IntArray, spec: VersionSpec): IntArray {
            val generator = reedSolomonGenerator(spec.ecPerBlock)
            val dataBlocks = mutableListOf<IntArray>()
            val ecBlocks = mutableListOf<IntArray>()

            var offset = 0
            repeat(spec.g1Blocks) {
                val block = data.copyOfRange(offset, offset + spec.g1Data)
                offset += spec.g1Data
                dataBlocks.add(block)
                ecBlocks.add(reedSolomonRemainder(block, generator))
            }
            repeat(spec.g2Blocks) {
                val block = data.copyOfRange(offset, offset + spec.g2Data)
                offset += spec.g2Data
                dataBlocks.add(block)
                ecBlocks.add(reedSolomonRemainder(block, generator))
            }

            val result = IntArray(spec.totalCodewords)
            var outIdx = 0
            val maxDataLen = maxOf(spec.g1Data, spec.g2Data)
            for (i in 0 until maxDataLen) {
                for (block in dataBlocks) {
                    if (i < block.size) result[outIdx++] = block[i]
                }
            }
            for (i in 0 until spec.ecPerBlock) {
                for (ec in ecBlocks) {
                    result[outIdx++] = ec[i]
                }
            }
            return result
        }

        private fun gfMul(a: Int, b: Int): Int {
            if (a == 0 || b == 0) return 0
            return GF_EXP[GF_LOG[a] + GF_LOG[b]]
        }

        private fun reedSolomonGenerator(degree: Int): IntArray {
            var poly = intArrayOf(1)
            for (i in 0 until degree) {
                val next = IntArray(poly.size + 1)
                val root = GF_EXP[i]
                for (j in poly.indices) {
                    next[j] = next[j] xor poly[j]
                    next[j + 1] = next[j + 1] xor gfMul(poly[j], root)
                }
                poly = next
            }
            return poly
        }

        private fun reedSolomonRemainder(data: IntArray, generator: IntArray): IntArray {
            val degree = generator.size - 1
            val rem = IntArray(degree)
            for (byte in data) {
                val factor = byte xor rem[0]
                for (i in 0 until degree - 1) rem[i] = rem[i + 1]
                rem[degree - 1] = 0
                if (factor != 0) {
                    for (i in 0 until degree) {
                        rem[i] = rem[i] xor gfMul(generator[i + 1], factor)
                    }
                }
            }
            return rem
        }

        private fun renderBestMask(codewords: IntArray, spec: VersionSpec): QrCodeMatrix {
            val size = spec.version * 4 + 17
            val baseModules = BooleanArray(size * size)
            val isFunction = BooleanArray(size * size)

            fun setFunction(x: Int, y: Int, dark: Boolean) {
                if (x in 0 until size && y in 0 until size) {
                    baseModules[y * size + x] = dark
                    isFunction[y * size + x] = true
                }
            }

            // Finder patterns + separators
            fun drawFinder(cx: Int, cy: Int) {
                for (dy in -4..4) {
                    for (dx in -4..4) {
                        val x = cx + dx
                        val y = cy + dy
                        if (x !in 0 until size || y !in 0 until size) continue
                        val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                        val dark = dist != 2 && dist != 4
                        setFunction(x, y, dark)
                    }
                }
            }
            drawFinder(3, 3)
            drawFinder(size - 4, 3)
            drawFinder(3, size - 4)

            // Alignment patterns
            val centers = spec.alignmentCenters
            for (cy in centers) {
                for (cx in centers) {
                    val isNearFinder =
                        (cx <= 8 && cy <= 8) ||
                            (cx >= size - 9 && cy <= 8) ||
                            (cx <= 8 && cy >= size - 9)
                    if (isNearFinder) continue
                    for (dy in -2..2) {
                        for (dx in -2..2) {
                            val dist = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                            setFunction(cx + dx, cy + dy, dist != 1)
                        }
                    }
                }
            }

            // Timing patterns
            for (i in 8 until size - 8) {
                if (!isFunction[6 * size + i]) setFunction(i, 6, i % 2 == 0)
                if (!isFunction[i * size + 6]) setFunction(6, i, i % 2 == 0)
            }

            // Reserve format info areas & dark module
            for (i in 0..8) {
                if (!isFunction[8 * size + i]) isFunction[8 * size + i] = true
                if (!isFunction[i * size + 8]) isFunction[i * size + 8] = true
            }
            for (i in 0 until 8) {
                isFunction[8 * size + (size - 1 - i)] = true
                isFunction[(size - 1 - i) * size + 8] = true
            }
            setFunction(8, size - 8, true)

            // Version info for versions >= 7
            if (spec.version >= 7) {
                var rem = spec.version
                repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
                val bits = (spec.version shl 12) or rem
                for (i in 0 until 18) {
                    val dark = ((bits ushr i) and 1) != 0
                    val a = size - 11 + i % 3
                    val b = i / 3
                    setFunction(a, b, dark)
                    setFunction(b, a, dark)
                }
            }

            // Place data bits in zigzag
            var bitIndex = 0
            val totalBits = codewords.size * 8
            var right = size - 1
            var upward = true
            while (right >= 1) {
                if (right == 6) right--
                for (step in 0 until size) {
                    val y = if (upward) size - 1 - step else step
                    for (colOffset in 0..1) {
                        val x = right - colOffset
                        if (!isFunction[y * size + x]) {
                            val dark = if (bitIndex < totalBits) {
                                ((codewords[bitIndex ushr 3] ushr (7 - (bitIndex and 7))) and 1) != 0
                            } else false
                            baseModules[y * size + x] = dark
                            bitIndex++
                        }
                    }
                }
                right -= 2
                upward = !upward
            }

            var bestPenalty = Int.MAX_VALUE
            var bestModules = baseModules

            for (mask in 0..7) {
                val candidate = baseModules.copyOf()
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        if (!isFunction[y * size + x] && shouldFlipMask(mask, x, y)) {
                            candidate[y * size + x] = !candidate[y * size + x]
                        }
                    }
                }
                applyFormatBits(candidate, size, mask)
                val penalty = computePenalty(candidate, size)
                if (penalty < bestPenalty) {
                    bestPenalty = penalty
                    bestModules = candidate
                }
            }

            return QrCodeMatrix(size, bestModules)
        }

        private fun shouldFlipMask(mask: Int, x: Int, y: Int): Boolean = when (mask) {
            0 -> (x + y) % 2 == 0
            1 -> y % 2 == 0
            2 -> x % 3 == 0
            3 -> (x + y) % 3 == 0
            4 -> (y / 2 + x / 3) % 2 == 0
            5 -> (x * y) % 2 + (x * y) % 3 == 0
            6 -> ((x * y) % 2 + (x * y) % 3) % 2 == 0
            7 -> ((x + y) % 2 + (x * y) % 3) % 2 == 0
            else -> false
        }

        private fun applyFormatBits(modules: BooleanArray, size: Int, mask: Int) {
            // ECC Level M = 00 -> 5-bit data = (00 << 3) | mask
            val data = (0b00 shl 3) or mask
            var rem = data
            repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
            val bits = ((data shl 10) or rem) xor 0x5412

            // Top-left vertical + horizontal
            for (i in 0..5) modules[i * size + 8] = ((bits ushr i) and 1) != 0
            modules[7 * size + 8] = ((bits ushr 6) and 1) != 0
            modules[8 * size + 8] = ((bits ushr 7) and 1) != 0
            modules[8 * size + 7] = ((bits ushr 8) and 1) != 0
            for (i in 9..14) modules[8 * size + (14 - i)] = ((bits ushr i) and 1) != 0

            // Top-right & bottom-left
            for (i in 0..7) modules[8 * size + (size - 1 - i)] = ((bits ushr i) and 1) != 0
            for (i in 8..14) modules[(size - 15 + i) * size + 8] = ((bits ushr i) and 1) != 0
        }

        private fun computePenalty(modules: BooleanArray, size: Int): Int {
            var penalty = 0
            // Rule 1: runs of 5+ same color
            for (y in 0 until size) {
                var runColor = modules[y * size]
                var runLen = 1
                for (x in 1 until size) {
                    val c = modules[y * size + x]
                    if (c == runColor) {
                        runLen++
                        if (runLen == 5) penalty += 3 else if (runLen > 5) penalty++
                    } else {
                        runColor = c
                        runLen = 1
                    }
                }
            }
            for (x in 0 until size) {
                var runColor = modules[x]
                var runLen = 1
                for (y in 1 until size) {
                    val c = modules[y * size + x]
                    if (c == runColor) {
                        runLen++
                        if (runLen == 5) penalty += 3 else if (runLen > 5) penalty++
                    } else {
                        runColor = c
                        runLen = 1
                    }
                }
            }
            // Rule 2: 2x2 blocks
            for (y in 0 until size - 1) {
                for (x in 0 until size - 1) {
                    val c = modules[y * size + x]
                    if (c == modules[y * size + x + 1] &&
                        c == modules[(y + 1) * size + x] &&
                        c == modules[(y + 1) * size + x + 1]
                    ) {
                        penalty += 3
                    }
                }
            }
            return penalty
        }
    }
}
