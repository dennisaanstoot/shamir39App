package com.example.test

import java.lang.Exception
import java.lang.Integer.parseInt
import java.lang.RuntimeException

class Shamir {

    var VERSION = "shamir39-p1"

    // Splits a BIP39 mnemonic into Shamir39 mnemonics.
    // No validation is done on the bip39 words.
    fun split(
        bip39MnemonicWords: List<String>,
        wordlist: List<String>,
        m: Int,
        n: Int
    ): List<Array<String>> {
        // validate inputs
        if (m < 2) {
            error("Must require at least 2 shares")
        }
        if (m > 4095) {
            error("Must require at most 4095 shares")
        }
        if (n < 2) {
            error("Must split to at least 2 shares")
        }
        if (n > 4095) {
            error("Must split to at most 4095 shares")
        }
        // TODO make wordlist length more general
        if (wordlist.size != 2048) {
            error("Wordlist must have 2048 words")
        }
        if (bip39MnemonicWords.size == 0) {
            error("No bip39 mnemonic words provided")
        }
        // convert bip39 mnemonic into bits
        var binStr = ""
        for (i in 0 until bip39MnemonicWords.size) {
            val w = bip39MnemonicWords[i]
            val index = wordlist.indexOf(w)
            if (index == -1) {
                error("Invalid word found in list: " + w)
            }
            var bits = index.toString(2)
            bits = lpad(bits, 11)
            binStr = binStr + bits
        }
        // pad mnemonic for use as hex
        val lenForHex = Math.ceil(binStr.length / 4.0).toInt() * 4
        binStr = lpad(binStr, lenForHex)
        // convert to hex string
        val totalHexChars = binStr.length / 4
        var hexStr = ""
        for (i in 0 until totalHexChars) {
            val nibbleStr = binStr.substring(i * 4, (i + 1) * 4)
            val hexValue = nibbleStr.toInt(2)
            val hexChar = hexValue.toString(16)
            hexStr = hexStr + hexChar
        }
        // create shamir parts
        val partsHex = share(hexStr, n, m, 0, true)
        // convert parts into shamir39 mnemonics
        val mnemonics = ArrayList<Array<String>>()
        for (o in 0 until partsHex.size) {
            // set mnemonic version
            var mnemonic = arrayOf(VERSION)
            // set mnemonic parameters
            val parametersBin = paramsToBinaryStr(m, o)
            val paramsWords = binToMnemonic(parametersBin, wordlist)
            mnemonic = mnemonic + paramsWords
            // set mnemonic shamir part
            val partHex = partsHex[o]
            val partBin = hex2bin(partHex)
            val partWords = binToMnemonic(partBin, wordlist)
            mnemonic = mnemonic + partWords
            // add mnemonic part to mnemonics
            mnemonics.add(mnemonic)
        }
        return mnemonics
    }

    // Combines Shamir39 mnemonics into a BIP39 mnemonic
    fun combine(parts: List<List<String>>, wordlist: List<String>): List<String> {
        // convert parts to hex
        val hexParts = ArrayList<String>()
        var requiredParts = -1
        for (i in 0 until parts.size) {
            val words = parts[i]
            // validate version
            if (words[0] != VERSION) {
                error("Version doesn't match")
            }
            // get params
            var mBinStr = ""
            var oBinStr = ""
            var endParamsIndex = 1
            for (j in 1 until words.size) {
                val word = words[j]
                val wordIndex = wordlist.indexOf(word)
                if (wordIndex == -1) {
                    error("Word not in wordlist: " + word)
                }
                val wordBin = lpad(wordIndex.toString(2), 11)
                mBinStr = mBinStr + wordBin.substring(1, 6)
                oBinStr = oBinStr + wordBin.substring(6, 11)
                val isEndOfParams = wordBin[0] == '0'
                if (isEndOfParams) {
                    endParamsIndex = j
                    break
                }
            }
            // parse parameters
            val m = mBinStr.toInt(2)
            val o = oBinStr.toInt(2)
            // validate parameters
            if (requiredParts == -1) {
                requiredParts = m
            }
            if (m != requiredParts) {
                error("Inconsisent M parameters")
            }
            // get shamir part
            var partBin = ""
            for (j in endParamsIndex + 1 until words.size) {
                val word = words[j]
                val wordIndex = wordlist.indexOf(word)
                if (wordIndex == -1) {
                    error("Word not in wordlist: " + word)
                }
                val wordBin = lpad(wordIndex.toString(2), 11)
                partBin = partBin + wordBin
            }
            val hexChars = (partBin.length / 4) * 4
            val diff = partBin.length - hexChars
            partBin = partBin.substring(diff)
            val partHex = bin2hex(partBin)
            // insert in correct order and remove duplicates
            hexParts[o] = partHex
        }
        // remove missing parts
        val partsClean = ArrayList<Share>()
        for (i in 0 until hexParts.size) {
            if (hexParts[i] != null) {
                val partClean = Share(null, i+1, hexParts[i])
                partsClean.add(partClean)
            }
        }
        // validate the parameters to ensure the secret can be created
        if (partsClean.size < requiredParts) {
            error("Not enough parts, requires " + requiredParts)
        }
        // combine parts into secret
        val secretHex = combine(partsClean)
        // convert secret into mnemonic
        var secretBin = hex2bin(secretHex)
        val totalWords = Math.floor(secretBin.length / 11.0).toInt()
        val totalBits = totalWords * 11
        val diff = secretBin.length - totalBits
        secretBin = secretBin.substring(diff)
        val mnemonic = ArrayList<String>()
        for (i in 0 until totalWords) {
            val wordIndexBin = secretBin.substring(i * 11, (i + 1) * 11)
            val wordIndex = parseInt(wordIndexBin, 2)
            val word = wordlist[wordIndex]
            mnemonic.add(word)
        }
        return mnemonic
    }

    // encodes the paramaters into a binary string
    fun paramsToBinaryStr(m: Int, o: Int): String {
        // get m as binary, padded to multiple of 5 bits
        var mBin = m.toString(2)
        // get o as binary, padded to multiple of 5 bits
        var oBin = o.toString(2)
        // calculate the overall binary length of each parameter, which must
        // be identical
        val mBinFinalLength = Math.ceil(mBin.length / 5.0).toInt() * 5
        val oBinFinalLength = Math.ceil(oBin.length / 5.0).toInt() * 5
        val binFinalLength = Math.max(mBinFinalLength, oBinFinalLength)
        // pad each parameter
        mBin = lpad(mBin, binFinalLength)
        oBin = lpad(oBin, binFinalLength)
        // encode parameters in binary
        val totalWords = oBin.length / 5
        var binStr = ""
        for (i in 0 until totalWords) {
            val isLastWord = i == totalWords - 1
            var leadingBit = "1"
            if (isLastWord) {
                leadingBit = "0"
            }
            val mBits = mBin.substring(i * 5, (i + 1) * 5)
            val oBits = oBin.substring(i * 5, (i + 1) * 5)
            binStr = binStr + leadingBit + mBits + oBits
        }
        return binStr
    }

    fun binToMnemonic(binStr: String, wordlist: List<String>): List<String> {
        val mnemonic = ArrayList<String>()
        // pad binary to suit words of 11 bits
        val totalWords = Math.ceil(binStr.length / 11.0).toInt()
        val totalBits = totalWords * 11
        val binString = lpad(binStr, totalBits)
        // convert bits to words
        for (i in 0 until totalWords) {
            val bits = binString.substring(i * 11, (i + 1) * 11)
            val wordIndex = parseInt(bits, 2)
            val word = wordlist[wordIndex]
            mnemonic.add(word)
        }
        return mnemonic
    }

    // left-pad a number with zeros
    fun lpad(s: String, n: Int): String {
        var retval = s
        while (retval.length < n) {
            retval = "0" + retval
        }
        return retval
    }

    // Shamir functions modified from
    // https://github.com/amper5and/secrets.js/
    // by Alexander Stetsyuk - released under MIT License
    object Defaults {
        val bits = 8 // default number of bits
        val radix = 16 // work with HEX by default
        val minBits = 3
        val maxBits =
            20 // this permits 1,048,575 shares, though going this high is NOT recommended in JS!

        val bytesPerChar = 2
        val maxBytesPerChar = 6 // Math.pow(256,7) > Math.pow(2,53)

        // Primitive polynomials (in decimal form) for Galois Fields GF(2^n), for 2 <= n <= 30
        // The index of each term in the array corresponds to the n for that polynomial
        // i.e. to get the polynomial for n=16, use primitivePolynomials[16]
        val primitivePolynomials = arrayOf(
            null,
            null,
            1,
            3,
            3,
            5,
            3,
            3,
            29,
            17,
            9,
            5,
            83,
            27,
            43,
            3,
            45,
            9,
            39,
            39,
            9,
            5,
            3,
            33,
            27,
            9,
            71,
            39,
            9,
            5,
            83
        )

        // warning for insecure PRNG
        val warning =
            "WARNING:\nA secure random number generator was not found.\nUsing Math.random(), which is NOT cryptographically strong!"
    }

    // Protected settings object
    object Config {
        var rng: ((Int) -> String)? = null
        lateinit var exps: List<Int>
        lateinit var logs: List<Int>
        var radix: Int? = null
        var bits: Int? = null
        var size: Int? = null
        var max: Int? = null
        var unsafePRNG: Boolean? = null

        var alert: Int? = null
    }

    fun init(bits: Int?) {
        if (bits != null && (bits % 1 != 0 || bits < Defaults.minBits || bits > Defaults.maxBits)) {
            throw RuntimeException("Number of bits must be an integer between " + Defaults.minBits + " and " + Defaults.maxBits + ", inclusive.")
        }

        Config.radix = Defaults.radix
        Config.bits = bits ?: Defaults.bits
        Config.size = Math.pow(2.0, Config.bits!!.toDouble()).toInt()
        Config.max = Config.size!! - 1

        // Construct the exp and log tables for multiplication.
        val logs = ArrayList<Int>()
        val exps = ArrayList<Int>()
        var x = 1
        val primitive = Defaults.primitivePolynomials[Config.bits!!]!!
        for (i in 0 until Config.size!!) {
            exps[i] = x
            logs[x] = i
            x = x shl 1
            if (x >= Config.size!!) {
                x = x xor primitive
                x = x and Config.max!!
            }
        }

        Config.logs = logs
        Config.exps = exps
    }

    fun isInited(): Boolean {
        return true
    }

    // Returns a pseudo-random number generator of the form function(bits){}
    // which should output a random string of 1's and 0's of length `bits`
    fun getRNG(): (Int) -> String {

        fun construct(bits: Int, arr: Array<Int>, size: Int): String? {
            var str = ""
            var i = 0
            val len = arr.size - 1
            while (i < len || (str.length < bits)) {
                str += padLeft(arr[i].toString(2), size)
                i++
            }
            str = str.substring(-bits)
            if (str.all { c -> c == '0' }) { // all zeros?
                return null
            } else {
                return str
            }
        }


        // A totally insecure RNG!!! (except in Safari)
        // Will produce a warning every time it is called.
        Config.unsafePRNG = true
        warn()

        val bitsPerNum = 32
        val max = pow(2, bitsPerNum) - 1
        return { bits: Int ->
            val elems = Math.ceil(bits / bitsPerNum.toDouble()).toInt()
            val arr = Array(elems) { 0 }
            var str: String? = null
            while (str == null) {
                for (i in 0 until elems) {
                    arr[i] = Math.floor(Math.random() * max + 1).toInt()
                }
                str = construct(bits, arr, bitsPerNum)
            }
            str!!
        }
    }

    // Warn about using insecure rng.
    // Called when Math.random() is being used.
    fun warn() {
        println(Defaults.warning)
        Exception("Warning triggered").printStackTrace()
//        window['console']['warn'](defaults.warning);
//        if (typeof window['alert'] === 'function' && config.alert){
//            window['alert'](defaults.warning);
//        }
    }

    // Set the PRNG to use. If no RNG function is supplied, pick a default using getRNG()
    fun setRNG(rng: ((Int) -> String)?, alert: Int?): Boolean? {
        if (!isInited()) {
            init(null)
        }
        Config.unsafePRNG = false
        val newRng = rng ?: getRNG()

        // test the RNG (5 times)
        Config.rng = newRng
        if (alert != null)
            Config.alert = alert

        return Config.unsafePRNG
    }

    fun isSetRNG(): Boolean {
        return Config.rng != null
    }

    // Generates a random bits-length number string using the PRNG
    fun random(bits: Int): String {
        if (!isSetRNG()) {
            setRNG(null, null)
        }

        if (Config.unsafePRNG!!) {
            warn()
        }
        return bin2hex(Config.rng!!.invoke(bits))
    }

    // Divides a `secret` number String str expressed in radix `inputRadix` (optional, default 16)
    // into `numShares` shares, each expressed in radix `outputRadix` (optional, default to `inputRadix`),
    // requiring `threshold` number of shares to reconstruct the secret.
    // Optionally, zero-pads the secret to a length that is a multiple of padLength before sharing.
    fun share(
        secret: String,
        numShares: Int,
        threshold: Int,
        padLen: Int?,
        withoutPrefix: Boolean
    ): List<String> {
        if (!isInited()) {
            init(null)
        }
        if (!isSetRNG()) {
            setRNG(null, null)
        }

        val padLength = padLen ?: 0

        if (numShares % 1 != 0 || numShares < 2) {
            error("Number of shares must be an integer between 2 and 2^bits-1 (" + Config.max + "), inclusive.")
        }
        if (numShares > Config.max!!) {
            val neededBits = Math.ceil(Math.log(numShares + 1.0) / Math.log(2.toDouble()))
            error("Number of shares must be an integer between 2 and 2^bits-1 (" + Config.max + "), inclusive. To create " + numShares + " shares, use at least " + neededBits + " bits.")
        }
        if (threshold % 1 != 0 || threshold < 2) {
            error("Threshold number of shares must be an integer between 2 and 2^bits-1 (" + Config.max + "), inclusive.")
        }
        if (threshold > Config.max!!) {
            val neededBits = Math.ceil(Math.log(threshold + 1.0) / Math.log(2.0))
            error("Threshold number of shares must be an integer between 2 and 2^bits-1 (" + Config.max + "), inclusive.  To use a threshold of " + threshold + ", use at least " + neededBits + " bits.")
        }
        if (padLength % 1 != 0) {
            error("Zero-pad length must be an integer greater than 1.")
        }

        if (Config.unsafePRNG!!) {
            warn()
        }

        val newSecret =
            '1' + hex2bin(secret) // append a 1 so that we can preserve the correct number of leading zeros in our secret
        val newSecretList = split(newSecret, padLength)
        val x: MutableList<String> = ArrayList(newSecretList.map { it.toString() })
        val y: Array<String?> = Array(numShares) { null }
        val len = newSecret.length
        for (i in 0 until len) {
            val subShares = _getShares(newSecretList[i], numShares, threshold)
            for (j in 0 until numShares) {
                x[j] = x[j] ?: subShares[j].x.toString(Config.radix!!)
                y[j] = padLeft(subShares[j].y.toString(2), null) + (if (y[j] != null) y[j] else "")
            }
        }
        val padding = Config.max!!.toString(Config.radix!!).length
        if (withoutPrefix) {
            for (i in 0 until numShares) {
                x[i] = bin2hex(y[i]!!)
            }
        } else {
            for (i in 0 until numShares) {
                x[i] = Config.bits!!.toString(36).toUpperCase() + padLeft(
                    x[i],
                    padding
                ) + bin2hex(y[i]!!)
            }
        }

        return x
    }

    // This is the basic polynomial generation and evaluation function
    // for a `config.bits`-length secret (NOT an arbitrary length)
    // Note: no error-checking at this stage! If `secrets` is NOT
    // a NUMBER less than 2^bits-1, the output will be incorrect!
    fun _getShares(secret: Int, numShares: Int, threshold: Int): List<PointShare> {
        val shares = ArrayList<PointShare>()
        val coeffs = mutableListOf(secret)

        for (i in 1 until threshold) {
            coeffs[i] = parseInt(Config.rng!!.invoke(Config.bits!!), 2)
        }
        val len = numShares + 1
        for (i in 1 until len) {
            shares[i - 1] = PointShare(i, horner(i, coeffs))
        }
        return shares
    }

    data class PointShare(var x: Int, var y: Int)

    // Polynomial evaluation at `x` using Horner's Method
    // TODO: this can possibly be sped up using other methods
    // NOTE: fx=fx * x + coeff[i] ->  exp(log(fx) + log(x)) + coeff[i],
    //       so if fx===0, just set fx to coeff[i] because
    //       using the exp/log form will result in incorrect value
    fun horner(x: Int, coeffs: List<Int>): Int {
        val logx = Config.logs[x]
        var fx = 0
        var i = coeffs.size - 1
        while (i >= 0) {
            if (fx == 0) {
                fx = coeffs[i]
                continue
            }
            fx = pow(Config.exps[(logx + Config.logs[fx]) % Config.max!!], coeffs[i])
            i--
        }
        return fx
    }

    fun <T> inArray(arr: Array<T>, value: T): Boolean {
        val len = arr.size
        for (i in 0 until len) {
            if (arr[i] == value) {
                return true
            }
        }
        return false
    }

    fun processShare(share: Share): Share {

        val bits = Config.bits!!

        val max = pow(2, bits) - 1
        var idLength = max.toString(Config.radix!!).length

        val id = share.id
        val part = share.value
        return Share(bits, id, part)
    }

    data class Share(var bits: Int?, var id: Int, var value: String)

    // Protected method that evaluates the Lagrange interpolation
    // polynomial at x=`at` for individual config.bits-length
    // segments of each share in the `shares` Array.
    // Each share is expressed in base `inputRadix`. The output
    // is expressed in base `outputRadix'
    fun _combine(at: Int, shares: List<Share>): String {
        var setBits: Int? = null
        var share: Share
        val x = ArrayList<Int>()
        val y = ArrayList<ArrayList<Int>?>()
        var result = ""
        var idx: Int
        var len = shares.size
        for (i in 0 until len) {
            share = processShare(shares[i])
            if (setBits == null) {
                setBits = share.bits
            } else if (share.bits != setBits) {
                error("Mismatched shares: Different bit settings.")
            }

            if (Config.bits != setBits) {
                init(setBits)
            }

            if (inArray(x.toArray(), share.id)) { // repeated x value?
                continue
            }
            x.add(share.id)
            idx = x.size - 1
            val shareArr = split(hex2bin(share.value), null)
            val len2 = shareArr.size
            for (j in 0 until len2) {
                y[j] = if (y[j] != null) y[j] else ArrayList()
                y[j]!![idx] = shareArr[j]
            }
        }
        len = y.size
        for (i in 0 until len) {
            val x1 = x.toTypedArray()
            val y1 = y[i]!!.toTypedArray()
            result = padLeft(lagrange(at, x1, y1).toString(2), null) + result
        }

        if (at == 0) {// reconstructing the secret
            idx = result.indexOf('1') //find the first 1
            return bin2hex(result.slice(0 until idx + 1))
        } else {// generating a new share
            return bin2hex(result)
        }
    }

    // Combine `shares` Array into the original secret
    fun combine(shares: List<Share>): String {
        return _combine(0, shares)
    }

    // Evaluate the Lagrange interpolation polynomial at x = `at`
    // using x and y Arrays that are of the same length, with
    // corresponding elements constituting points on the polynomial.
    fun lagrange(at: Int, x: Array<Int>, y: Array<Int>): Int {
        var sum = 0
        var product: Int
        val len = x.size
        for (i in 0 until len) {
            if (y[i] == 0) {
                continue
            }

            product = Config.logs[y[i]].toInt()
            for (j in 0 until len) {
                if (i == j) {
                    continue
                }
                if (at == x[j]) { // happens when computing a share that is in the list of shares used to compute it
                    product =
                        -1 // fix for a zero product term, after which the sum should be sum^0 = sum, not sum^1
                    break
                }
                product =
                    (product + Config.logs[pow(at, x[j])] - Config.logs[pow(
                        x[i],
                        x[j]
                    )] + Config.max!!/* to make sure it's not negative */) % Config.max!!
            }

            sum = if (product == -1) sum else pow(
                sum,
                Config.exps[product].toInt()
            ) // though exps[-1]= undefined and undefined ^ anything = anything in chrome, this behavior may not hold everywhere, so do the check
        }
        return sum
    }

    fun pow(x: Int, p: Int): Int {
        var retval = 1
        for (i in 0 until p) {
            retval *= x
        }
        return retval
    }

    // Splits a number string `bits`-length segments, after first
    // optionally zero-padding it to a length that is a multiple of `padLength.
    // Returns array of integers (each less than 2^bits-1), with each element
    // representing a `bits`-length segment of the input string from right to left,
    // i.e. parts[0] represents the right-most `bits`-length segment of the input string.
    fun split(str: String, padLength: Int?): List<Int> {
        var newStr = str
        if (padLength != null) {
            newStr = padLeft(str, padLength)
        }
        val parts = ArrayList<Int>()
        var i = newStr.length
        val bits = Config.bits!!
        while (i > bits) {
            parts.add(parseInt(str.slice(i - bits until i), 2))
            i -= bits
        }
        parts.add(parseInt(str.slice(0 until i), 2))
        return parts
    }

    // Pads a string `str` with zeros on the left so that its length is a multiple of `bits`
    fun padLeft(str: String, bits: Int?): String {
        val newBits = (bits ?: Config.bits)!!
        val missing = str.length % newBits
        return (if (missing != 0) (0 until (newBits - missing + 1)).map { i -> "0" }
            .joinToString(separator = "") else "") + str
    }

    fun hex2bin(str: String): String {
        var bin = ""
        var num: Int
        var i = str.length - 1
        while (i >= 0) {
            num = parseInt(str[i].toString(), 16)
            bin = padLeft(num.toString(2), 4) + bin
            i--
        }
        return bin
    }

    fun bin2hex(str: String): String {
        var hex = ""
        var num: Int
        val newStr = padLeft(str, 4)
        var i = newStr.length
        while (i >= 4) {
            num = parseInt(newStr.slice(i - 4 until i), 2)
            hex = num.toString(16) + hex
            i -= 4
        }
        return hex
    }

    // Converts a given UTF16 character string to the HEX representation.
    // Each character of the input string is represented by
    // `bytesPerChar` bytes in the output string.
    fun str2hex(str: String, bytesPerChar: Int?): String {
        val newBytesPerChar = bytesPerChar ?: Defaults.bytesPerChar

        val hexChars = 2 * newBytesPerChar
        val max = Math.pow(16.0, hexChars.toDouble()).toInt() - 1
        var out = ""
        val len = str.length
        for (i in 0 until len) {
            val num = str[i].toInt()
            if (num > max) {
                val neededBytes = Math.ceil(Math.log(num + 1.0) / Math.log(256.0))
                throw RuntimeException("Invalid character code (" + num + "). Maximum allowable is 256^bytes-1 (" + max + "). To convert this character, use at least " + neededBytes + " bytes.")
            } else {
                out = padLeft(num.toString(16), hexChars) + out
            }
        }
        return out
    }

    // Converts a given HEX number string to a UTF16 character string.
    fun hex2str(str: String, bytesPerChar: Int?): String {
        val newBytesPerChar = bytesPerChar ?: Defaults.bytesPerChar

        val hexChars = 2 * newBytesPerChar
        var out = ""
        val newStr = padLeft(str, hexChars)
        val len = str.length
        var i = 0
        while (i < len) {
            out = parseInt(newStr.slice(i until i + hexChars), 16).toChar() + out
            i += hexChars
        }
        return out
    }

}