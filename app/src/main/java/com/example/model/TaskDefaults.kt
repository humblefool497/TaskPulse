package com.example.model

object TaskDefaults {
    val DEFAULT_CATEGORIES = listOf(
        "Official",
        "Personal",
        "Finance/Bills",
        "Health",
        "Family",
        "Education/Kids",
        "Shopping/Errands",
        "Travel",
        "Home/Maintenance",
        "Learning/Skill",
        "Legal/Compliance",
        "Meetings",
        "Other"
    )

    val DEFAULT_ORIGINS = listOf(
        "Self",
        "Boss",
        "Client",
        "Email",
        "Meeting",
        "Phone Call",
        "Family",
        "Vendor",
        "Team"
    )

    data class TaskTemplate(
        val templateName: String,
        val title: String,
        val description: String,
        val criticality: Criticality,
        val actionNeeded: ActionNeeded,
        val category: String,
        val subtasks: List<String> = emptyList(),
        val recurrence: RecurrenceType = RecurrenceType.NONE
    )

    val PRESET_TEMPLATES = listOf(
        TaskTemplate(
            templateName = "Monthly Bill Payment",
            title = "Pay Monthly Utilities & Bills",
            description = "Review invoices, verify charges, and process payment via online banking.",
            criticality = Criticality.HIGH,
            actionNeeded = ActionNeeded.TO_ACT_MYSELF,
            category = "Finance/Bills",
            subtasks = listOf("Download electric invoice", "Check credit card statement", "Authorize payment", "Archive receipts"),
            recurrence = RecurrenceType.MONTHLY
        ),
        TaskTemplate(
            templateName = "Client Project Follow-up",
            title = "Follow up with Client on Deliverable Approval",
            description = "Send follow-up email and schedule quick check-in regarding the pending sign-off.",
            criticality = Criticality.CRITICAL,
            actionNeeded = ActionNeeded.FOLLOW_UP_NEEDED,
            category = "Official",
            subtasks = listOf("Check email thread", "Draft progress summary", "Send follow-up note", "Log client response")
        ),
        TaskTemplate(
            templateName = "Team Delegation / Assign",
            title = "Assign Milestone Deliverable to Team Member",
            description = "Brief assignee with requirements, reference files, and deadline expectations.",
            criticality = Criticality.MEDIUM,
            actionNeeded = ActionNeeded.ASSIGN_TO_OTHERS,
            category = "Official",
            subtasks = listOf("Prepare brief & assets", "Share task specs", "Confirm acceptance")
        ),
        TaskTemplate(
            templateName = "Quarterly Tax & Compliance",
            title = "File Quarterly Compliance & Tax Return",
            description = "Collate income expenses, generate reports, and submit filings to tax authority.",
            criticality = Criticality.CRITICAL,
            actionNeeded = ActionNeeded.IMMEDIATE_ATTENTION,
            category = "Legal/Compliance",
            subtasks = listOf("Export expense ledger", "Reconcile invoices", "Submit tax portal filing")
        ),
        TaskTemplate(
            templateName = "Routine Home Maintenance",
            title = "HVAC & Home Safety Inspection",
            description = "Replace air filters, test smoke alarms, and schedule annual technician service.",
            criticality = Criticality.LOW,
            actionNeeded = ActionNeeded.TO_ACT_MYSELF,
            category = "Home/Maintenance",
            subtasks = listOf("Inspect filters", "Test alarms", "Check water filter")
        )
    )
}
