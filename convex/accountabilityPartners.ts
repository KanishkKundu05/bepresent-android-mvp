import { mutation, query, internalAction } from "./_generated/server";
import { internal } from "./_generated/api";
import { v } from "convex/values";

/**
 * List all accountability partners for the current user.
 */
export const list = query({
  args: {},
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) return [];

    const user = await ctx.db
      .query("users")
      .withIndex("by_externalId", (q) => q.eq("externalId", identity.subject))
      .unique();
    if (!user) return [];

    const partners = await ctx.db
      .query("accountabilityPartners")
      .withIndex("by_userId", (q) => q.eq("userId", user._id))
      .collect();

    return partners.map((p) => ({
      id: p._id,
      contactName: p.contactName,
      phoneNumber: p.phoneNumber,
      createdAt: p.createdAt,
    }));
  },
});

/**
 * Add a new accountability partner from phone contacts.
 */
export const add = mutation({
  args: {
    contactName: v.string(),
    phoneNumber: v.string(),
  },
  handler: async (ctx, { contactName, phoneNumber }) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) throw new Error("Not authenticated");

    const user = await ctx.db
      .query("users")
      .withIndex("by_externalId", (q) => q.eq("externalId", identity.subject))
      .unique();
    if (!user) throw new Error("User not found");

    // Deduplicate by phone number
    const existing = await ctx.db
      .query("accountabilityPartners")
      .withIndex("by_user_phone", (q) =>
        q.eq("userId", user._id).eq("phoneNumber", phoneNumber)
      )
      .unique();
    if (existing)
      throw new Error("This contact is already your accountability partner");

    const partnerId = await ctx.db.insert("accountabilityPartners", {
      userId: user._id,
      contactName,
      phoneNumber,
      createdAt: Date.now(),
    });

    // Schedule SMS notification
    await ctx.scheduler.runAfter(
      0,
      internal.accountabilityPartners.sendSms,
      {
        phoneNumber,
        contactName,
        userName: user.displayName,
      }
    );

    return partnerId;
  },
});

/**
 * Remove an accountability partner.
 */
export const remove = mutation({
  args: { partnerId: v.id("accountabilityPartners") },
  handler: async (ctx, { partnerId }) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) throw new Error("Not authenticated");

    const user = await ctx.db
      .query("users")
      .withIndex("by_externalId", (q) => q.eq("externalId", identity.subject))
      .unique();
    if (!user) throw new Error("User not found");

    const partner = await ctx.db.get(partnerId);
    if (!partner || partner.userId !== user._id)
      throw new Error("Partner not found");

    await ctx.db.delete(partnerId);
  },
});

/**
 * Send SMS via Twilio REST API (internal only — called by scheduler).
 */
export const sendSms = internalAction({
  args: {
    phoneNumber: v.string(),
    contactName: v.string(),
    userName: v.string(),
  },
  handler: async (_ctx, { phoneNumber, contactName, userName }) => {
    const accountSid = process.env.TWILIO_ACCOUNT_SID;
    const authToken = process.env.TWILIO_AUTH_TOKEN;
    const fromNumber = process.env.TWILIO_PHONE_NUMBER;

    if (!accountSid || !authToken || !fromNumber) {
      console.error("Twilio credentials not configured — skipping SMS");
      return;
    }

    const message = `Hey ${contactName}! ${userName} added you as their accountability partner on BePresent. Help them stay focused!`;

    const url = `https://api.twilio.com/2010-04-01/Accounts/${accountSid}/Messages.json`;

    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: "Basic " + btoa(`${accountSid}:${authToken}`),
      },
      body: new URLSearchParams({
        To: phoneNumber,
        From: fromNumber,
        Body: message,
      }).toString(),
    });

    if (!response.ok) {
      const errorBody = await response.text();
      console.error(`Twilio SMS failed: ${response.status} — ${errorBody}`);
    }
  },
});
