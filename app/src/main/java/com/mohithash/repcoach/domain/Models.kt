package com.mohithash.repcoach.domain

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val goal: String = "hypertrophy",       // strength | hypertrophy | fat loss | general
    val experience: String = "beginner",    // beginner | intermediate | advanced
    val daysPerWeek: Int = 3,
    val equipment: String = "full gym",     // full gym | dumbbells | bodyweight
    val minutes: Int = 60,
    val limitations: String = "",
    val onboarded: Boolean = false,
)

@Serializable data class PlannedExercise(val name: String, val sets: Int = 3, val reps: String = "8-12", val rest_s: Int = 90, val cue: String = "")
@Serializable data class PlanDay(val name: String, val focus: String = "", val exercises: List<PlannedExercise> = emptyList())
@Serializable data class Plan(val title: String = "", val rationale: String = "", val days: List<PlanDay> = emptyList(), val createdAt: Long = 0)

@Serializable data class ExerciseAdvice(val change: String = "", val why: String = "", val next_targets: List<String> = emptyList())
@Serializable data class Debrief(val summary: String = "", val highlights: List<String> = emptyList(), val advice: List<ExerciseAdvice> = emptyList(), val recovery_tip: String = "")
