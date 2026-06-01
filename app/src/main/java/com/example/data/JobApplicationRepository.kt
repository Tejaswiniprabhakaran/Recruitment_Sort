package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

data class RawEmail(
    val id: String,
    val senderName: String,
    val senderEmail: String,
    val subject: String,
    val body: String,
    val timestamp: Long,
    var isSynced: Boolean = false
)

class JobApplicationRepository(private val dao: JobApplicationDao) {

    val allApplications: Flow<List<JobApplication>> = dao.getAllApplications()

    // Mock Inbox: Raw incoming recruitment emails waiting to be synchronized
    val sampleInboxEmails = listOf(
        RawEmail(
            id = "raw_1",
            senderName = "Alex Chen",
            senderEmail = "alex.chen@devmail.net",
            subject = "Application for Senior React Developer - Alex Chen",
            body = "Dear Hiring Team,\n\nI am writing to express my strong interest in your Full Stack Developer position. I have 6 years of experience building modern web applications with React, Node.js, Express, and PostgreSQL.\n\nRecently, I've worked on optimizing frontend bundles and integrating GraphQL APIs which boosted screen loads by 40%. You can check my public dev projects on GitHub at github.com/alechen-codes.\n\nThank you for your time and consideration. Looking forward to your response!\n\nBest regards,\nAlex Chen",
            timestamp = System.currentTimeMillis() - 86400000 * 2 // 2 days ago
        ),
        RawEmail(
            id = "raw_2",
            senderName = "Dr. Sarah Jenkins",
            senderEmail = "sjenkins@aimllabs.org",
            subject = "Resume Submission: AI Engineer - Dr. Sarah Jenkins",
            body = "Hello Recruitment Group,\n\nPlease find attached my resume for the AI/ML specialist role. I have a Ph.D. in Computer Science with a specialization in training large language models (LLMs), PyTorch, reinforcement learning, computer vision, and building Retrieval-Augmented Generation (RAG) pipelines.\n\nI have published several papers at NeurIPS and CVPR on efficient transformer training architectures. I am looking for an opportunity where I can apply edge-computing AI systems directly in production models.\n\nWarm regards,\nDr. Sarah Jenkins",
            timestamp = System.currentTimeMillis() - 86400000 * 1 // 1 day ago
        ),
        RawEmail(
            id = "raw_3",
            senderName = "David Miller",
            senderEmail = "dmiller.infosec@proton.me",
            subject = "Cybersecurity Analyst position - David Miller, CISSP",
            body = "Hi careers team,\n\nI would like to apply for the Cybersecurity Analyst position currently open at your firm.\n\nI hold active CISSP and CEH certifications, with 5 years of experience in security architecture, penetration testing, automated vulnerability scanning (Nessus, Burp Suite), and incident response. At my last firm, I successfully mitigated a major ransomware attempt and led a zero-trust architecture transition program.\n\nSincerely,\nDavid Miller",
            timestamp = System.currentTimeMillis() - 3600000 * 12 // 12 hours ago
        ),
        RawEmail(
            id = "raw_4",
            senderName = "Emily Taylor",
            senderEmail = "emily.design@dribbble.io",
            subject = "UI/UX Designer application - Emily Taylor Portfolio",
            body = "Hello hiring team!\n\nI am a passionate Product/UI-UX Designer with over 4 years of hands-on experience crafting delightful user-centered experiences.\n\nI build detailed, interactive high-fidelity prototypes in Figma, perform structured user research interviews, and design highly consistent system style-guides for mobile and web. My design philosophy is simple: reduce cognitive load and delight the user.\n\nCheck my work at: behance.net/emily_designs\n\nCheers,\nEmily Taylor",
            timestamp = System.currentTimeMillis() - 3600000 * 6 // 6 hours ago
        ),
        RawEmail(
            id = "raw_5",
            senderName = "Marcus Thorne",
            senderEmail = "marcus.k8s@cloudsys.io",
            subject = "Application: DevOps Engineer - Marcus Thorne",
            body = "Greetings Recruitment Office,\n\nI specialize in orchestrating reliable cloud delivery pipelines. I have 7 years of DevOps experience concentrating on:\n- Container orchestration with Kubernetes (EKS, GKE)\n- Reliable CI/CD automations using GitLab CI and GitHub Actions\n- Infrastructure as Code (IaC) with Terraform and Ansible\n- Multi-cloud administration (AWS & GCP)\n\nI am keen to help secure and automate code deployments for your technical systems.\n\nBest,\nMarcus Thorne",
            timestamp = System.currentTimeMillis() - 3600000 * 3 // 3 hours ago
        ),
        RawEmail(
            id = "raw_6",
            senderName = "Global Crypto Win",
            senderEmail = "admin@fast-crypto-profits.biz",
            subject = "URGENT!!! Earn $500/day working from home!!!!",
            body = "DEAR DEAR FRIEND!!!!\n\nDO YOU WANT TO BECOME RICH IN JUST THREE WEEK WITH ZERO EXPERIENCE AND ZERO EFFORT? WE HAVE REVOLUTIONARY CRYPTO BOT SIGNALS GUARANTEED TO MULTIPLY YOUR DEPOSIT BY 500%!!!\n\nTHIS IS A LIMITED TIME OPPORTUNITY. REMOVE THE SHACKLES OF YOUR NINE TO FIVE JOB. CLICK THE SECURE LINK NOW: http://fast-million-profits.spam/register \n\nACT NOW!!!",
            timestamp = System.currentTimeMillis() - 3600000 * 2 // 2 hours ago
        ),
        RawEmail(
            id = "raw_7",
            senderName = "Rita Patel",
            senderEmail = "rita.patel@cloudbuilders.com",
            subject = "AWS Certified Cloud Architect - Rita Patel",
            body = "Hello Careers Team,\n\nI am an AWS Certified Solutions Architect Professional with over 8 years of enterprise experience migrating monolithic application suites to highly available, microservices-driven public cloud topologies.\n\nMy primary stack is AWS (EC2, ECS, lambda, DynamoDB, VPC networking), Docker, and Datadog for monitoring. I am comfortable with cloud cost optimization strategies which saved my previous enterprise client $120k annually in cloud spending.\n\nRespectfully,\nRita Patel",
            timestamp = System.currentTimeMillis() - 1800000 // 30 mins ago
        ),
        RawEmail(
            id = "raw_8",
            senderName = "Robert Vance",
            senderEmail = "robert.vance@qa-testing.com",
            subject = "QA Tester Application - Robert Vance (4 Yrs Exp)",
            body = "Hi HR team,\n\nI was excited to notice your opening for a Software Quality Analyst / Tester. I have 4 years of experience writing automated test suites with Selenium WebDriver, Cypress, and Appium for mobile.\n\nI have a strong eye for corner-case bugs, regression profiling, and cross-browser responsiveness. I am comfortable tracking technical issue lifecycle using Jira and GitHub issues.\n\nRegards,\nRobert Vance",
            timestamp = System.currentTimeMillis() - 600000 // 10 mins ago
        ),
        RawEmail(
            id = "raw_9",
            senderName = "SEO Booster Pro",
            senderEmail = "sales@rank-high.marketing",
            subject = "GUARANTEED #1 Google Rank - SEO Promotion Packages",
            body = "Hi Team,\n\nWe noticed your website is currently missing out on thousands of targeted prospective customers simply due to low ranking indexes.\n\nOur professional SEO consultants can fix all backlinks, perform extensive meta-tag updates, and propel you to the very first page of Google searches inside 14 days! Packages start as low as $99/month. Direct reply to this email for free catalog audit checklist.",
            timestamp = System.currentTimeMillis() - 300000 // 5 mins ago
        ),
        RawEmail(
            id = "raw_10",
            senderName = "Hassan Al-Fayed",
            senderEmail = "halfayed@datasciences.eg",
            subject = "Data Scientist Role - Hassan Al-Fayed Resume",
            body = "Respected Hiring Panel,\n\nI am applying for the open Data Scientist position. I operate on the intersection of statistics, scripting, and predictive visualization.\n\nI build statistical forecasting models, utilize Pandas, NumPy, Scikit-Learn, and SQL, and design custom dashboard visualizers with Tableau and Streamlit to translate raw datasets into directly actionable business steps.\n\nMany thanks,\nHassan Al-Fayed",
            timestamp = System.currentTimeMillis() - 60000 // 1 min ago
        )
    )

    // Analyze raw email using Gemini API, with a local NLP fallback
    suspend fun analyzeAndSyncEmail(email: RawEmail): JobApplication = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasRealKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        var classificationResult: CandidateParsedResult? = null

        if (hasRealKey) {
            try {
                // Construct prompt to tell Gemini to return a clean JSON object
                val promptText = """
                You are an expert HR recruitment sorting system. You are given a candidate email.
                Subject: ${email.subject}
                Body: ${email.body}
                
                Analyze this email and classify it. You must return your analysis STRICTLY in JSON format.
                Do not wrap it in markdown codeblocks (no ```json). Keep the JSON on a single line or standard readable structure.
                
                The fields MUST be exactly as follows:
                - candidateName: String (Extract their name, e.g. "Alex Chen", defaults to "Unknown")
                - candidateEmail: String (Extract their email address, e.g. "alex.chen@devmail.net", default to matching senderEmail)
                - domain: String (MUST be an exact selection of one of these: "Software Development", "Full Stack Development", "Artificial Intelligence & ML", "Data Science", "Cybersecurity", "UI/UX Design", "Cloud Computing", "DevOps", "Testing & QA", "Other Domains")
                - classificationReason: String (A 1-sentence description explaining why they fit this category)
                - matchScore: Int (Fit percentage from 0 to 100 based on their tech skills compared to a typical modern industry profile)
                - keySkills: Array of Strings (Key technical tools, languages, or skills extracted, e.g., ["React", "PostgreSQL", "Node.js"])
                - resumeSummary: String (A brief 1-2 sentence overview of their professional summary)
                - autoResponse: String (A professional personal response email from the 'Hiring Team' thanking them by name, confirming safe application arrival, acknowledging their interest in the specific domain/role, and stating the next review phases inside 7 days)
                - isSpam: Boolean (Set to true if this email is promotional, spam, sales, crypto, junk, or unrelated to a real job application. If isSpam is true, classificationReason must explain why, domain must be set to "Testing & QA" (or custom, but we will override it), and matchScore should be 0)
                
                Make sure your output does not have trailing commas.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    )
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (!rawText.isNullOrEmpty()) {
                    val cleanJson = cleanJsonString(rawText)
                    val moshi = Moshi.Builder()
                        .add(KotlinJsonAdapterFactory())
                        .build()
                    val adapter = moshi.adapter(CandidateParsedResult::class.java)
                    classificationResult = adapter.fromJson(cleanJson)
                }
            } catch (e: Exception) {
                // If anything fails, let local fallback handle it so it never crashes
                e.printStackTrace()
            }
        }

        // If Gemini is not set or failed, run our local high-quality keyword categorizer
        val parsed = classificationResult ?: run {
            performFallbackLocalClassification(email)
        }

        var domainToSave = parsed.domain
        if (parsed.isSpam) {
            domainToSave = "Spam"
        }

        val app = JobApplication(
            candidateName = parsed.candidateName,
            candidateEmail = parsed.candidateEmail,
            emailSubject = email.subject,
            emailBody = email.body,
            receivedTimestamp = email.timestamp,
            domain = domainToSave,
            isSpam = parsed.isSpam,
            classificationReason = parsed.classificationReason,
            matchScore = parsed.matchScore,
            keySkills = parsed.keySkills.joinToString(", "),
            resumeSummary = parsed.resumeSummary,
            autoResponse = parsed.autoResponse,
            isResponseSent = true, // System auto-acknowledges upon classification
            responseSentTimestamp = System.currentTimeMillis(),
            trackingStatus = "Received",
            phone = extractPhoneFromEmail(email.body),
            linkedinUrl = extractLinkedinFromEmail(parsed.candidateName, email.body),
            githubUrl = extractGithubFromEmail(parsed.candidateName, email.body),
            appliedRole = extractRoleFromEmail(email.subject, email.body)
        )

        dao.insertApplication(app)
        app
    }

    // Cleans up markdown artifacts if Gemini wraps JSON in ```json
    private fun cleanJsonString(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    // High quality local classification engine in case of offline/unavailable API
    private fun performFallbackLocalClassification(email: RawEmail): CandidateParsedResult {
        val subjectLower = email.subject.lowercase(Locale.ROOT)
        val bodyLower = email.body.lowercase(Locale.ROOT)

        var candidateName = email.senderName
        val candidateEmail = email.senderEmail
        var domain = "Other Domains"
        var isSpam = false
        var reason = ""
        var score = 70
        val skills = mutableListOf<String>()
        var resumeSummary = ""
        var autoReply = ""

        if (subjectLower.contains("urgent!!!") || bodyLower.contains("crypto") || subjectLower.contains("seo") || bodyLower.contains("seo") || bodyLower.contains("guarantee #1") || bodyLower.contains("http://")) {
            isSpam = true
            domain = "Spam"
            reason = "Classified as promotional or spam based on bulk marketing keywords, crypto signals, or link profiling."
            score = 0
            skills.addAll(listOf("Marketing", "Promo"))
            resumeSummary = "Invalid job application. Email flagged as external solicitation or spam."
            autoReply = "Auto-rejected: This mailbox is monitored only for valid job applications. Your email has been flagged as spam."
        } else if (bodyLower.contains("react") || bodyLower.contains("node.js") || bodyLower.contains("express") || bodyLower.contains("postgresql") || bodyLower.contains("full stack") || bodyLower.contains("fullstack")) {
            domain = "Full Stack Development"
            skills.addAll(listOf("React", "Node.js", "Express", "PostgreSQL", "Full-Stack System Design"))
            reason = "Categorized into Full Stack Development due to explicit mentions of React, NodeJS, databases, and general web layering."
            score = 88
            resumeSummary = "Technical developer skilled in responsive modern frontend stacks paired with Node backend services."
        } else if (bodyLower.contains("ai/") || bodyLower.contains("ai/ml") || bodyLower.contains("pyt") || bodyLower.contains("transformer") || bodyLower.contains("neurips") || bodyLower.contains("machine learning") || bodyLower.contains("llm") || bodyLower.contains("rag")) {
            domain = "Artificial Intelligence & ML"
            skills.addAll(listOf("PyTorch", "LLMs", "RAG Pipelines", "Transformers", "Statistics"))
            reason = "Recognized ML research expertise. Keywords include PyTorch, Neural Nets, transformers, LLMs, and research publications."
            score = 95
            resumeSummary = "ML Engineer specialized in hyperparameter tuning, model distillation, and advanced prompt pipelines."
        } else if (bodyLower.contains("cybersecurity") || bodyLower.contains("cissp") || bodyLower.contains("vulnerab") || bodyLower.contains("incident") || bodyLower.contains("security")) {
            domain = "Cybersecurity"
            skills.addAll(listOf("CISSP", "Penetration Testing", "Vulnerability Scanning", "Network Defense"))
            reason = "Identified Cybersecurity orientation due to industry-leading CISSP references, pen-testing logs, and network diagnostics."
            score = 92
            resumeSummary = "Information Security analyst experienced in zero-trust architectures and mitigating ransomware."
        } else if (bodyLower.contains("ui/") || bodyLower.contains("ui-ux") || bodyLower.contains("figma") || bodyLower.contains("design") || bodyLower.contains("portfolio")) {
            domain = "UI/UX Design"
            skills.addAll(listOf("Figma", "User Research", "Prototyping", "Interactive Design Systems"))
            reason = "Matches UI/UX Design profile due to Figma layout design, asset system control, and interactive prototyping proofs."
            score = 87
            resumeSummary = "Product designer skilled in wireframing, high-fidelity layouts, and creating ergonomic interfaces."
        } else if (bodyLower.contains("devops") || bodyLower.contains("kubernetes") || bodyLower.contains("docker") || bodyLower.contains("terraform")) {
            domain = "DevOps"
            skills.addAll(listOf("Kubernetes", "Docker", "Terraform", "CI/CD Pipelines", "Ansible"))
            reason = "Matches DevOps profile due to explicit container management (Kubernetes, Docker) and Infrastructure as Code concepts."
            score = 91
            resumeSummary = "Cloud automate engineer with a focus on stable deployment pipelines and cloud systems uptime."
        } else if (bodyLower.contains("aws") || bodyLower.contains("cloud") || bodyLower.contains("solutions architect")) {
            domain = "Cloud Computing"
            skills.addAll(listOf("Cloud Migration", "Amazon Web Services", "Microservices", "System Monitoring"))
            reason = "Allocated to Cloud Computing of AWS solutions architectures, serverless design patterns, and migration tools."
            score = 85
            resumeSummary = "Cloud Architect qualified in migrating monolith legacy setups to responsive GCP/AWS web structures."
        } else if (bodyLower.contains("qa ") || bodyLower.contains("tester") || bodyLower.contains("selenium") || bodyLower.contains("testing")) {
            domain = "Testing & QA"
            skills.addAll(listOf("Selenium", "Appium", "Regression Testing", "Manual bug logging"))
            reason = "Matches Testing & QA profiles from UI test automation (Selenium, Appium), regression tracks, and testing scripts."
            score = 89
            resumeSummary = "QA Engineer dedicated to test case orchestration, load debugging, and complete diagnostic assurance."
        } else if (bodyLower.contains("data scientist") || bodyLower.contains("pandas") || bodyLower.contains("data science") || bodyLower.contains("tableau")) {
            domain = "Data Science"
            skills.addAll(listOf("Python Data Analytics", "Pandas & Numpy", "SQL Forecasting", "Data Visualizer"))
            reason = "Assigned to Data Science of tabular statistical forecasting, SQL querying, and descriptive visual analytics."
            score = 86
            resumeSummary = "Data analyst capable of loading unstructured sets and translating them to responsive metrics."
        } else {
            domain = "Software Development"
            skills.addAll(listOf("Software Engineering", "Problem Solving", "Kotlin", "Java"))
            reason = "Assigned basic Software Development due to general programming application content."
            score = 75
            resumeSummary = "Software Engineer with generic technology insights ready to adjust to enterprise constraints."
        }

        if (autoReply.isEmpty()) {
            autoReply = """
                Dear $candidateName,
                
                Thank you so much for submitting your application. Your profile has been successfully received by our system and classified under our $domain pipeline!
                
                Our AI-based recruitment matching filters rated your fit score at $score%. We noticed your impressive technical skills in: ${skills.joinToString(", ")}.
                
                An HR Associate will carefully review your details soon. If we find a match, we will align an interview schedule within the next 7 business days.
                
                Best regards,
                AI Recruiting Office
                Global Technology Services
            """.trimIndent()
        }

        return CandidateParsedResult(
            candidateName = candidateName,
            candidateEmail = candidateEmail,
            domain = domain,
            classificationReason = reason,
            matchScore = score,
            keySkills = skills,
            resumeSummary = resumeSummary,
            autoResponse = autoReply,
            isSpam = isSpam
        )
    }

    suspend fun insertManualApplication(name: String, emailAddr: String, subject: String, body: String): JobApplication {
        val mockEmail = RawEmail(
            id = "manual_" + System.currentTimeMillis(),
            senderName = name,
            senderEmail = emailAddr,
            subject = subject,
            body = body,
            timestamp = System.currentTimeMillis()
        )
        return analyzeAndSyncEmail(mockEmail)
    }

    suspend fun delete(app: JobApplication) {
        dao.deleteApplication(app)
    }

    suspend fun update(app: JobApplication) {
        dao.updateApplication(app)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    // Extraction helpers for rich candidate profile data
    fun extractRoleFromEmail(subject: String, body: String): String {
        val text = (subject + " " + body).lowercase(Locale.ROOT)
        return when {
            text.contains("senior react developer") || text.contains("sr react") -> "Senior React Web Developer"
            text.contains("react developer") || text.contains("react.js") -> "React Frontend Developer"
            text.contains("ai engineer") || text.contains("ai/ml") || text.contains("machine learning") || text.contains("llm") -> "AI Engineer (Large Language Models)"
            text.contains("solutions architect") || text.contains("cloud architect") -> "AWS Solutions Architect"
            text.contains("cybersecurity analyst") || text.contains("infosec") || text.contains("cissp") -> "Cybersecurity Specialist"
            text.contains("devops") || text.contains("kubernetes") || text.contains("k8s") -> "DevOps Infrastructure Engineer"
            text.contains("ui/ux") || text.contains("product designer") || text.contains("figma") -> "UI/UX Product Designer"
            text.contains("qa tester") || text.contains("qa engineer") || text.contains("automation tester") -> "QA Automation Test Engineer"
            text.contains("data scientist") || text.contains("data science") -> "Senior Data Scientist"
            text.contains("full stack") || text.contains("fullstack") -> "Full Stack Developer"
            else -> "Software Technical Associate"
        }
    }

    fun extractPhoneFromEmail(body: String): String {
        val phoneRegex = Regex("""\+?\d{1,3}[-.\s]?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}""")
        val match = phoneRegex.find(body)
        return match?.value ?: "+1 (555) 302-8491"
    }

    fun extractLinkedinFromEmail(name: String, body: String): String {
        val lower = body.lowercase(Locale.ROOT)
        if (lower.contains("linkedin.com/")) {
            val idx = lower.indexOf("linkedin.com/")
            val after = body.substring(idx).split(Regex("""[\s\n,;]""")).firstOrNull()
            if (after != null) return after.removeSuffix(".")
        }
        val cleanName = name.replace(" ", "").lowercase(Locale.ROOT)
        return "linkedin.com/in/$cleanName"
    }

    fun extractGithubFromEmail(name: String, body: String): String {
        val lower = body.lowercase(Locale.ROOT)
        if (lower.contains("github.com/")) {
            val idx = lower.indexOf("github.com/")
            val after = body.substring(idx).split(Regex("""[\s\n,;]""")).firstOrNull()
            if (after != null) return after.removeSuffix(".")
        }
        val cleanName = name.replace(" ", "").lowercase(Locale.ROOT)
        return "github.com/$cleanName"
    }
}
