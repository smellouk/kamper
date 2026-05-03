package com.smellouk.kamper.api

/**
 * Custom developer-logged event payload (D-13). Carried by KamperEvent(moduleName="event")
 * to Sentry, Firebase Crashlytics, and OpenTelemetry integration adapters.
 *
 * @property name Event name (e.g., "purchase", "video_decode"). Caller-owned; never
 *   blank in valid records — see [INVALID] sentinel.
 * @property durationMs `null` = instant event (D-16); non-null = completed duration
 *   slice in milliseconds (D-17). Computed by Engine.endEvent from start/end nanos.
 */
public data class UserEventInfo(
    val name: String,
    val durationMs: Long?
) : Info {
    public companion object {
        /**
         * Sentinel guarded by integration `onEvent()` `event.info === Info.INVALID`
         * checks (D-14). Equality contract: `UserEventInfo("", null) == INVALID`.
         */
        public val INVALID: UserEventInfo = UserEventInfo("", null)
    }
}
