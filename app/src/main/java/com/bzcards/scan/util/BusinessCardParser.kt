package com.bzcards.scan.util

import com.bzcards.scan.model.BusinessCard

object BusinessCardParser {

    private val EMAIL_REGEX = Regex(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    )
    // More aggressive phone regex that finds phones even when jammed together
    private val PHONE_REGEX = Regex(
        """(?:(?:\+\d{1,3}[\s.-]?)?(?:\(?\d{3}\)?[\s.-]?)?\d{3}[\s.-]?\d{4})"""
    )
    // Matches labeled phone entries like "CO: (608)..." or "C: (720)..." even when jammed together
    private val LABELED_PHONE_REGEX = Regex(
        """(?i)(?:phone|tel|cell|mobile|fax|office|direct|ph|co|c|m|f|o|d|t)\s*[:.]?\s*(\(?\d{3}\)?[\s.-]?\d{3}[\s.-]?\d{4})""",
    )
    private val WEBSITE_REGEX = Regex(
        """(?:https?://)?(?:www\.)?[a-zA-Z0-9][\w\-]*\.[a-zA-Z]{2,}(?:\.[a-zA-Z]{2,})?(?:/[^\s]*)?"""
    )
    private val ADDRESS_NUMBER_STREET_REGEX = Regex(
        """\d+\s+[\w\s]+(?:street|st\.?|avenue|ave\.?|boulevard|blvd\.?|road|rd\.?|drive|dr\.?|lane|ln\.?|way|place|pl\.?|court|ct\.?|circle|cir\.?|parkway|pkwy\.?)""",
        RegexOption.IGNORE_CASE
    )
    private val STATE_ZIP_REGEX = Regex(
        """\b[A-Z]{2}[\s,]+\d{5}(?:-\d{4})?\b"""
    )

    private val JOB_TITLE_KEYWORDS = listOf(
        "manager", "director", "engineer", "developer", "designer", "analyst",
        "consultant", "specialist", "coordinator", "administrator", "president",
        "vp", "vice president", "ceo", "cto", "cfo", "coo", "cio", "chief",
        "officer", "lead", "head of", "senior", "junior", "associate",
        "architect", "supervisor", "executive", "founder", "co-founder", "partner",
        "assistant", "secretary", "intern", "technician", "representative",
        "accountant", "attorney", "lawyer", "professor", "teacher", "nurse",
        "realtor", "broker", "agent", "planner", "strategist", "advisor",
        "recruiter", "buyer", "owner", "proprietor", "principal",
        "superintendent", "foreman", "captain", "sergeant", "lieutenant"
    )

    private val COMPANY_INDICATORS = listOf(
        "inc", "inc.", "llc", "ltd", "ltd.", "corp", "corp.", "corporation",
        "company", "co.", "group", "holdings", "solutions", "services",
        "technologies", "technology", "tech", "consulting", "enterprises",
        "associates", "international", "industries", "agency", "studio",
        "studios", "labs", "laboratory", "laboratories", "partners",
        "foundation", "institute", "university", "college", "school",
        "hospital", "clinic", "medical", "dental", "law firm", "firm",
        "bank", "financial", "insurance", "realty", "properties",
        "construction", "builders", "electric", "plumbing", "roofing",
        "landscaping", "automotive", "motors", "church", "ministries",
        "software"
    )

    private val ADDRESS_KEYWORDS = listOf(
        "street", "st.", "ave", "avenue", "blvd", "boulevard", "road", "rd.",
        "drive", "dr.", "lane", "ln.", "way", "suite", "ste.", "ste",
        "floor", "fl.", "#", "box", "p.o.", "po box", "unit", "apt",
        "building", "bldg", "place", "pl.", "court", "ct.", "circle",
        "parkway", "pkwy", "highway", "hwy"
    )

    // Labels that precede data on cards (e.g., "E: email@..." or "Phone: 555...")
    // Note: single-letter labels like "C:" or "O:" only match before phone-like content,
    // handled separately to avoid stripping meaningful text
    private val LABEL_REGEX = Regex(
        """^(?:email|mail|phone|tel|cell|mobile|fax|office|direct|web|website|url|addr|address)\s*[:.]\s*""",
        RegexOption.IGNORE_CASE
    )

    fun parse(rawText: String): BusinessCard {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Pre-process: merge lines that OCR split (names, companies, titles)
        val mergedLines = preprocessLines(lines)

        // First pass: extract high-confidence structured data
        val emails = extractAllEmails(rawText)
        val phones = extractAllPhones(mergedLines, rawText)
        val websites = extractAllWebsites(rawText, emails)

        // Tag each line by what type of data it contains
        val lineTags = mergedLines.map { line -> tagLine(line, emails, phones, websites) }

        // Extract fields using tagged lines
        val email = emails.firstOrNull() ?: ""
        val phone = phones.joinToString(", ")
        val website = websites.firstOrNull() ?: ""
        val jobTitle = extractJobTitle(mergedLines, lineTags)
        val company = extractCompany(mergedLines, lineTags, jobTitle, email)
        val address = extractAddress(mergedLines, lineTags)
        val name = extractName(mergedLines, lineTags, jobTitle, company)

        return BusinessCard(
            name = name,
            jobTitle = jobTitle,
            company = company,
            phone = phone,
            email = email,
            website = website,
            address = address,
            rawText = rawText
        )
    }

    private enum class LineTag {
        EMAIL, PHONE, WEBSITE, ADDRESS, JOB_TITLE, COMPANY, NAME_CANDIDATE, UNKNOWN
    }

    /**
     * Merge consecutive lines that OCR split apart.
     * Handles split names ("AHMED" + "SAYED"), companies ("Yahara g" + "SOFTWARE"),
     * and job titles ("Principal Product" + "Owner").
     */
    private fun preprocessLines(lines: List<String>): List<String> {
        if (lines.size < 2) return lines
        val result = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (i + 1 < lines.size) {
                val nextLine = lines[i + 1]
                val combined = "$line $nextLine"

                // Merge consecutive ALL CAPS single-word lines as a name
                // e.g., "AHMED" + "SAYED" → "AHMED SAYED"
                if (isSingleCapWord(line) && isSingleCapWord(nextLine) &&
                    !isCompanyLine(line.lowercase()) && !isCompanyLine(nextLine.lowercase()) &&
                    !isJobTitleLine(line.lowercase()) && !isJobTitleLine(nextLine.lowercase())
                ) {
                    result.add(combined)
                    i += 2
                    continue
                }

                // Merge split company names (within first few lines)
                if (i < 4) {
                    val lineIsCompany = isCompanyLine(line.lowercase())
                    val nextIsCompany = isCompanyLine(nextLine.lowercase())

                    if (!lineIsCompany && nextIsCompany && nextLine.split("\\s+".toRegex()).size <= 2) {
                        result.add(combined)
                        i += 2
                        continue
                    }
                    if (lineIsCompany && !nextIsCompany && line.split("\\s+".toRegex()).size <= 2
                        && nextLine.split("\\s+".toRegex()).size <= 2
                        && !looksLikePersonName(nextLine)
                    ) {
                        result.add(combined)
                        i += 2
                        continue
                    }
                }

                // Merge split job titles
                // e.g., "Principal Product" + "Owner" → "Principal Product Owner"
                val lineIsTitle = isJobTitleLine(line.lowercase())
                val nextIsTitle = isJobTitleLine(nextLine.lowercase())
                if ((lineIsTitle || nextIsTitle) &&
                    !EMAIL_REGEX.containsMatchIn(nextLine) &&
                    !PHONE_REGEX.containsMatchIn(nextLine) &&
                    nextLine.split("\\s+".toRegex()).size <= 3 &&
                    line.split("\\s+".toRegex()).size <= 3 &&
                    isJobTitleLine(combined.lowercase()) &&
                    nextLine.all { it.isLetter() || it.isWhitespace() || it == '-' || it == '/' }
                ) {
                    // Only merge if one has title keywords and the other looks like a continuation
                    if (lineIsTitle && !nextIsTitle && !isCompanyLine(nextLine.lowercase())) {
                        result.add(combined)
                        i += 2
                        continue
                    }
                    if (!lineIsTitle && nextIsTitle && !isCompanyLine(line.lowercase())) {
                        result.add(combined)
                        i += 2
                        continue
                    }
                }
            }
            result.add(line)
            i++
        }
        return result
    }

    private fun isSingleCapWord(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.isNotBlank() &&
            !trimmed.contains("\\s".toRegex()) &&
            trimmed == trimmed.uppercase() &&
            trimmed != trimmed.lowercase() &&
            trimmed.all { it.isLetter() }
    }

    private fun tagLine(
        line: String,
        emails: List<String>,
        phones: List<String>,
        websites: List<String>
    ): Set<LineTag> {
        val tags = mutableSetOf<LineTag>()
        val lower = line.lowercase()

        if (EMAIL_REGEX.containsMatchIn(line)) tags.add(LineTag.EMAIL)

        // Check if line contains any of the extracted phone numbers
        if (phones.any { line.contains(it) } || lineContainsPhoneData(line)) {
            tags.add(LineTag.PHONE)
        }

        if (isWebsiteLine(line, emails)) tags.add(LineTag.WEBSITE)
        if (isAddressLine(line)) tags.add(LineTag.ADDRESS)
        if (isJobTitleLine(lower)) tags.add(LineTag.JOB_TITLE)
        if (isCompanyLine(lower)) tags.add(LineTag.COMPANY)

        // Tag lines that look like person names (ALL CAPS, 2-3 words, all letters)
        if (tags.isEmpty() && looksLikePersonName(line)) {
            tags.add(LineTag.NAME_CANDIDATE)
        }

        return tags
    }

    private fun lineContainsPhoneData(line: String): Boolean {
        // Check if line has labeled phones (CO:, C:, Phone:, etc.) or multiple phones
        if (LABELED_PHONE_REGEX.containsMatchIn(line)) return true
        val stripped = LABEL_REGEX.replace(line, "").trim()
        if (PHONE_REGEX.containsMatchIn(stripped)) {
            val digits = stripped.count { it.isDigit() }
            val total = stripped.length
            return total > 0 && digits.toFloat() / total > 0.4f
        }
        return false
    }

    private fun isWebsiteLine(line: String, emails: List<String>): Boolean {
        if (EMAIL_REGEX.containsMatchIn(line)) {
            val withoutEmails = emails.fold(line) { acc, email -> acc.replace(email, "") }
            return WEBSITE_REGEX.containsMatchIn(withoutEmails)
        }
        return WEBSITE_REGEX.containsMatchIn(line) && !line.contains("@")
    }

    private fun isAddressLine(line: String): Boolean {
        val lower = line.lowercase()
        if (ADDRESS_KEYWORDS.any { lower.contains(it) }) return true
        if (STATE_ZIP_REGEX.containsMatchIn(line)) return true
        if (ADDRESS_NUMBER_STREET_REGEX.containsMatchIn(line)) return true
        if (Regex("""[A-Za-z]+,?\s+[A-Z]{2}\s+\d{5}""").containsMatchIn(line)) return true
        return false
    }

    private fun isJobTitleLine(lowerLine: String): Boolean {
        return JOB_TITLE_KEYWORDS.any { keyword ->
            Regex("""\b${Regex.escape(keyword)}\b""").containsMatchIn(lowerLine)
        }
    }

    private fun isCompanyLine(lowerLine: String): Boolean {
        return COMPANY_INDICATORS.any { indicator ->
            Regex("""\b${Regex.escape(indicator)}\b""").containsMatchIn(lowerLine)
        }
    }

    private fun extractAllEmails(text: String): List<String> {
        return EMAIL_REGEX.findAll(text).map { it.value }.distinct().toList()
    }

    private fun extractAllPhones(lines: List<String>, rawText: String): List<String> {
        val results = mutableListOf<String>()

        // First: find all labeled phones in the entire raw text
        // This handles cases like "CO: (608) 821-1750C: (720) 341-1642"
        // where labels and phones are jammed together
        LABELED_PHONE_REGEX.findAll(rawText).forEach { match ->
            val phone = match.groupValues[1].trim()
            val digits = phone.replace(Regex("[^\\d]"), "")
            if (digits.length in 7..15) {
                results.add(phone)
            }
        }

        // Second: find unlabeled phones in each line
        for (line in lines) {
            if (EMAIL_REGEX.containsMatchIn(line) && !PHONE_REGEX.containsMatchIn(
                    line.replace(EMAIL_REGEX, "")
                )
            ) continue

            PHONE_REGEX.findAll(line).forEach { match ->
                val phone = match.value.trim()
                val digits = phone.replace(Regex("[^\\d]"), "")
                if (digits.length in 7..15 && !results.any { existing ->
                        existing.replace(Regex("[^\\d]"), "") == digits
                    }) {
                    results.add(phone)
                }
            }
        }

        return results.distinct()
    }

    private fun extractAllWebsites(text: String, emails: List<String>): List<String> {
        // Find websites that appear on their own (not as part of an email address)
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val results = mutableListOf<String>()
        for (line in lines) {
            // Skip lines that are purely an email address
            if (EMAIL_REGEX.matches(line.trim())) continue
            // Remove any email addresses from the line, then look for websites
            val lineWithoutEmails = EMAIL_REGEX.replace(line, "").trim()
            if (lineWithoutEmails.isBlank()) continue
            WEBSITE_REGEX.findAll(lineWithoutEmails).forEach { match ->
                val url = match.value
                if (!url.contains("@")) {
                    results.add(url)
                }
            }
        }
        return results.distinct()
    }

    private fun extractJobTitle(lines: List<String>, lineTags: List<Set<LineTag>>): String {
        for (i in lines.indices) {
            val tags = lineTags[i]
            if (LineTag.JOB_TITLE in tags &&
                LineTag.COMPANY !in tags &&
                LineTag.ADDRESS !in tags &&
                LineTag.EMAIL !in tags &&
                LineTag.PHONE !in tags &&
                LineTag.WEBSITE !in tags
            ) {
                return stripLabel(lines[i])
            }
        }
        for (i in lines.indices) {
            if (LineTag.JOB_TITLE in lineTags[i]) {
                return stripLabel(lines[i])
            }
        }
        return ""
    }

    private fun extractCompany(
        lines: List<String>,
        lineTags: List<Set<LineTag>>,
        jobTitle: String,
        email: String
    ): String {
        // Strategy 1: line tagged as COMPANY only
        for (i in lines.indices) {
            val tags = lineTags[i]
            val line = lines[i]
            if (LineTag.COMPANY in tags &&
                line != jobTitle &&
                LineTag.ADDRESS !in tags &&
                LineTag.EMAIL !in tags &&
                LineTag.PHONE !in tags &&
                LineTag.WEBSITE !in tags
            ) {
                return stripLabel(line)
            }
        }

        // Strategy 2: infer from email domain
        // e.g., jsrmagala@yaharasoftware.com → "yaharasoftware"
        if (email.isNotBlank()) {
            val domain = email.substringAfter("@").substringBefore(".")
            // Look for a line near the top that contains the domain name
            for (i in 0 until minOf(4, lines.size)) {
                val line = lines[i]
                val tags = lineTags[i]
                if (line.lowercase().contains(domain.lowercase()) &&
                    LineTag.EMAIL !in tags &&
                    LineTag.PHONE !in tags &&
                    LineTag.WEBSITE !in tags &&
                    line != jobTitle
                ) {
                    return stripLabel(line)
                }
            }
        }

        // Strategy 3: ALL CAPS line near the top
        for (i in 0 until minOf(4, lines.size)) {
            val line = lines[i]
            val tags = lineTags[i]
            if (line.length > 2 &&
                line == line.uppercase() &&
                line != line.lowercase() &&
                LineTag.EMAIL !in tags &&
                LineTag.PHONE !in tags &&
                LineTag.WEBSITE !in tags &&
                LineTag.ADDRESS !in tags &&
                LineTag.NAME_CANDIDATE !in tags &&
                line != jobTitle
            ) {
                return line
            }
        }
        return ""
    }

    private fun extractAddress(lines: List<String>, lineTags: List<Set<LineTag>>): String {
        val addressLines = mutableListOf<String>()
        for (i in lines.indices) {
            if (LineTag.ADDRESS in lineTags[i] &&
                LineTag.EMAIL !in lineTags[i] &&
                LineTag.WEBSITE !in lineTags[i]
            ) {
                addressLines.add(stripLabel(lines[i]))
            }
        }
        return addressLines.joinToString(", ")
    }

    private fun extractName(
        lines: List<String>,
        lineTags: List<Set<LineTag>>,
        jobTitle: String,
        company: String
    ): String {
        // Strategy 1: look for NAME_CANDIDATE tagged lines first
        // These are lines that look distinctly like person names (e.g., "JAMES SMAGALA")
        for (i in lines.indices) {
            val line = lines[i]
            val tags = lineTags[i]
            if (LineTag.NAME_CANDIDATE in tags && line != company && line != jobTitle) {
                return line
            }
        }

        // Strategy 2: first line with no tags that looks like a name
        for (i in lines.indices) {
            val line = lines[i]
            val tags = lineTags[i]
            if (tags.isEmpty() && line != jobTitle && line != company) {
                val stripped = stripLabel(line)
                if (looksLikeName(stripped)) {
                    return stripped
                }
            }
        }

        // Strategy 3: infer from email address
        // e.g., jsrmagala@... or james.smagala@... → look for matching line
        val emails = lines.flatMap { EMAIL_REGEX.findAll(it).toList() }.map { it.value }
        for (email in emails) {
            val localPart = email.substringBefore("@")
            // Try to match "firstname.lastname" or "firstlast" against lines
            val nameParts = localPart.split(".", "_", "-").filter { it.length > 1 }
            for (i in lines.indices) {
                val line = lines[i]
                val tags = lineTags[i]
                if (LineTag.PHONE !in tags && LineTag.EMAIL !in tags &&
                    LineTag.WEBSITE !in tags && LineTag.ADDRESS !in tags &&
                    line != company && line != jobTitle
                ) {
                    val lineLower = line.lowercase()
                    val matchCount = nameParts.count { part -> lineLower.contains(part.lowercase()) }
                    if (matchCount >= 1 && nameParts.isNotEmpty()) {
                        return line
                    }
                }
            }
        }

        // Last resort: first line that isn't phone/email/website/address
        for (i in lines.indices) {
            val tags = lineTags[i]
            if (LineTag.PHONE !in tags && LineTag.EMAIL !in tags &&
                LineTag.WEBSITE !in tags && LineTag.ADDRESS !in tags &&
                lines[i] != company && lines[i] != jobTitle
            ) {
                return lines[i]
            }
        }

        return lines.firstOrNull() ?: ""
    }

    /**
     * Checks if a line looks like a person's name.
     * Person names on business cards are often:
     * - 2-3 words, all capitalized or all UPPER CASE
     * - All letters (no digits, no special chars except hyphens/apostrophes)
     * - Not too long
     */
    private fun looksLikePersonName(text: String): Boolean {
        if (text.isBlank()) return false
        val words = text.split("\\s+".toRegex())
        if (words.size !in 2..4) return false
        // All words should be mostly letters and start with uppercase
        val allNameLike = words.all { word ->
            word.isNotEmpty() &&
                word[0].isUpperCase() &&
                word.all { it.isLetter() || it == '-' || it == '\'' || it == '.' }
        }
        if (!allNameLike) return false
        if (text.length > 35) return false
        if (text.count { it.isDigit() } > 0) return false
        // Should not match company indicators or job title keywords
        val lower = text.lowercase()
        if (isCompanyLine(lower)) return false
        if (isJobTitleLine(lower)) return false
        return true
    }

    private fun looksLikeName(text: String): Boolean {
        if (text.isBlank()) return false
        val words = text.split("\\s+".toRegex())
        if (words.size !in 1..5) return false
        val nameWords = words.count { word ->
            word.isNotEmpty() && (word[0].isUpperCase() || word.length <= 3) &&
                word.count { it.isLetter() || it == '-' || it == '\'' } >= word.length * 0.7
        }
        if (nameWords < words.size * 0.6) return false
        if (text.length > 40) return false
        if (text.count { it.isDigit() } > 1) return false
        return true
    }

    private fun stripLabel(line: String): String {
        return LABEL_REGEX.replace(line, "").trim()
    }
}
