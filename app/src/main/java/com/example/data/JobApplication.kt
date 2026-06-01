package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "job_applications")
data class JobApplication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val candidateName: String,
    val candidateEmail: String,
    val emailSubject: String,
    val emailBody: String,
    val receivedTimestamp: Long,
    val domain: String, // e.g. "Full Stack Development", "Artificial Intelligence & ML", "Spam", etc.
    val isSpam: Boolean,
    val classificationReason: String,
    val matchScore: Int, // 0 to 100
    val keySkills: String, // Comma-separated list
    val resumeSummary: String,
    val autoResponse: String,
    val isResponseSent: Boolean,
    val responseSentTimestamp: Long = 0L,
    val trackingStatus: String = "Received", // e.g., "Received", "Reviewed", "Shortlisted", "Offer", "Rejected"
    val phone: String = "+1 (555) 019-2834",
    val linkedinUrl: String = "",
    val githubUrl: String = "",
    val appliedRole: String = "Technical Candidate"
)
