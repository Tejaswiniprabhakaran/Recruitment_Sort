package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CandidateParsedResult(
    val candidateName: String,
    val candidateEmail: String,
    val domain: String,
    val classificationReason: String,
    val matchScore: Int,
    val keySkills: List<String>,
    val resumeSummary: String,
    val autoResponse: String,
    val isSpam: Boolean
)
