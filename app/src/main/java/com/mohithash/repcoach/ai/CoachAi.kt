package com.mohithash.repcoach.ai

import com.mohithash.repcoach.data.SetLog
import com.mohithash.repcoach.domain.Debrief
import com.mohithash.repcoach.domain.Plan
import com.mohithash.repcoach.domain.Profile

class CoachAi(private val client: AiClient) {
    private val planSchema = Schema.obj(
        "title" to Schema.str, "rationale" to Schema.str,
        "days" to Schema.arr(Schema.obj("name" to Schema.str, "focus" to Schema.str,
            "exercises" to Schema.arr(Schema.obj("name" to Schema.str, "sets" to Schema.int, "reps" to Schema.str, "rest_s" to Schema.int, "cue" to Schema.str)))),
    )
    private val debriefSchema = Schema.obj(
        "summary" to Schema.str, "highlights" to Schema.arr(Schema.str),
        "advice" to Schema.arr(Schema.obj("change" to Schema.str, "why" to Schema.str, "next_targets" to Schema.arr(Schema.str))), "recovery_tip" to Schema.str,
    )

    private fun who(p: Profile) = "Trainee: ${p.experience}, goal ${p.goal}, ${p.daysPerWeek} days/week, ${p.minutes} min/session, equipment: ${p.equipment}." +
        (if (p.limitations.isNotBlank()) " Limitations/injuries: ${p.limitations}." else "")

    suspend fun plan(ai: AiSettings, p: Profile): Plan {
        val system = """You are an evidence-based strength coach. Build a weekly programme with exactly ${p.daysPerWeek} training days that fits ${p.minutes} minutes each.
            |Use only exercises possible with the stated equipment. Respect limitations. 4-7 exercises per day. reps as a range like "6-8" or "10-15".
            |cue: one short form cue. rationale: 2 sentences on the split and progression rule. Use consistent, canonical exercise names (e.g. "Barbell Back Squat").""".trimMargin()
        val plan: Plan = client.ask(ai, system, who(p), planSchema, maxTokens = 6000)
        return plan.copy(createdAt = System.currentTimeMillis())
    }

    suspend fun debrief(ai: AiSettings, p: Profile, dayName: String, sets: List<SetLog>, previous: Map<String, List<SetLog>>): Debrief {
        val system = """You are a strength coach reviewing today's session. Compare to the previous session for each exercise.
            |summary: 1-2 sentences. highlights: up to 3 PRs or wins. advice: one entry per exercise that needs a change next time (load, reps, or technique) with a specific next_targets like "3×8 @ 62.5 kg"; skip exercises that should just repeat.
            |recovery_tip: one sentence. Progression rule of thumb: if all sets hit the top of the rep range, add 2.5 kg (upper) / 5 kg (lower) or 1-2 reps for bodyweight. ${who(p)}""".trimMargin()
        fun fmt(l: List<SetLog>) = l.groupBy { it.exercise }.entries.joinToString("\n") { (e, s) -> "$e: " + s.sortedBy { it.setIndex }.joinToString(", ") { "${it.weight}×${it.reps}" } }
        val user = "Today ($dayName):\n${fmt(sets)}\n\nPrevious for these exercises:\n" + previous.entries.joinToString("\n") { (e, s) -> "$e: " + s.joinToString(", ") { "${it.weight}×${it.reps}" } }.ifBlank { "(first time)" }
        return client.ask(ai, system, user, debriefSchema, maxTokens = 2500)
    }

    suspend fun howTo(ai: AiSettings, exercise: String): String =
        client.chat(ai, "You are a strength coach. Give setup, execution and the two most common mistakes for the exercise, as 5-7 short bullet points. No preamble.", listOf(ChatMsg("user", exercise)), maxTokens = 600)
}
