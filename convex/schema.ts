import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  users: defineTable({
    externalId: v.string(), // Auth0 sub
    name: v.optional(v.string()),
    displayName: v.string(),
    createdAt: v.number(),
  }).index("by_externalId", ["externalId"]),

  dailyStats: defineTable({
    userId: v.id("users"),
    date: v.string(), // "2026-02-15"
    totalXp: v.number(),
    totalCoins: v.number(),
    maxStreak: v.number(),
    sessionsCompleted: v.number(),
    totalFocusMinutes: v.number(),
  })
    .index("by_user_date", ["userId", "date"])
    .index("by_date", ["date"]),

  sessionHistory: defineTable({
    userId: v.id("users"),
    localSessionId: v.string(), // dedup key from Room
    name: v.string(),
    goalDurationMinutes: v.number(),
    state: v.string(),
    earnedXp: v.number(),
    startedAt: v.number(),
    endedAt: v.optional(v.number()),
    syncedAt: v.number(),
  }).index("by_user_localId", ["userId", "localSessionId"]),

  intentionSnapshots: defineTable({
    userId: v.id("users"),
    packageName: v.string(),
    appName: v.string(),
    streak: v.number(),
    allowedOpensPerDay: v.number(),
    totalOpensToday: v.number(),
    syncedAt: v.number(),
  }).index("by_user_package", ["userId", "packageName"]),

  partnerships: defineTable({
    requesterId: v.id("users"),
    partnerId: v.id("users"),
    status: v.string(), // "pending" | "accepted" | "rejected"
    createdAt: v.number(),
    respondedAt: v.optional(v.number()),
  })
    .index("by_requester", ["requesterId"])
    .index("by_partner", ["partnerId"])
    .index("by_pair", ["requesterId", "partnerId"]),

  friendCodes: defineTable({
    userId: v.id("users"),
    code: v.string(), // 6-char uppercase
  })
    .index("by_userId", ["userId"])
    .index("by_code", ["code"]),

  accountabilityPartners: defineTable({
    userId: v.id("users"),
    contactName: v.string(),
    phoneNumber: v.string(),
    createdAt: v.number(),
  })
    .index("by_userId", ["userId"])
    .index("by_user_phone", ["userId", "phoneNumber"]),

  appUsageDaily: defineTable({
    userId: v.id("users"),
    date: v.string(),             // "2026-03-01"
    packageName: v.string(),
    appName: v.string(),
    totalTimeMs: v.number(),
    openCount: v.number(),
    syncedAt: v.number(),
  })
    .index("by_user_date", ["userId", "date"])
    .index("by_user_package_date", ["userId", "packageName", "date"]),
});
