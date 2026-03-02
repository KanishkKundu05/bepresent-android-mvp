import { internalQuery, internalMutation, mutation } from "./_generated/server";
import { v } from "convex/values";

/** Look up a subscription by device ID. */
export const getSubscriptionByDevice = internalQuery({
  args: { deviceId: v.string() },
  handler: async (ctx, args) => {
    return await ctx.db
      .query("subscriptions")
      .withIndex("by_deviceId", (q) => q.eq("deviceId", args.deviceId))
      .unique();
  },
});

/** Create or update a subscription record. */
export const upsertSubscription = internalMutation({
  args: {
    deviceId: v.string(),
    stripeCustomerId: v.string(),
    stripeSubscriptionId: v.string(),
    status: v.string(),
    priceId: v.string(),
    currentPeriodEnd: v.number(),
  },
  handler: async (ctx, args) => {
    const existing = await ctx.db
      .query("subscriptions")
      .withIndex("by_deviceId", (q) => q.eq("deviceId", args.deviceId))
      .unique();

    if (existing) {
      await ctx.db.patch(existing._id, {
        stripeCustomerId: args.stripeCustomerId,
        stripeSubscriptionId: args.stripeSubscriptionId,
        status: args.status,
        priceId: args.priceId,
        currentPeriodEnd: args.currentPeriodEnd,
      });
      return existing._id;
    }

    return await ctx.db.insert("subscriptions", {
      deviceId: args.deviceId,
      stripeCustomerId: args.stripeCustomerId,
      stripeSubscriptionId: args.stripeSubscriptionId,
      status: args.status,
      priceId: args.priceId,
      currentPeriodEnd: args.currentPeriodEnd,
      createdAt: Date.now(),
    });
  },
});

/** Update subscription status from webhook events. */
export const updateSubscriptionStatus = internalMutation({
  args: {
    stripeSubscriptionId: v.string(),
    status: v.string(),
    currentPeriodEnd: v.optional(v.number()),
  },
  handler: async (ctx, args) => {
    const sub = await ctx.db
      .query("subscriptions")
      .withIndex("by_stripeSubscriptionId", (q) =>
        q.eq("stripeSubscriptionId", args.stripeSubscriptionId)
      )
      .unique();

    if (!sub) return;

    const patch: Record<string, unknown> = { status: args.status };
    if (args.currentPeriodEnd !== undefined) {
      patch.currentPeriodEnd = args.currentPeriodEnd;
    }
    await ctx.db.patch(sub._id, patch);
  },
});

/** Link an anonymous subscription to an authenticated user. Called after Auth0 login. */
export const linkSubscriptionToUser = mutation({
  args: { deviceId: v.string() },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) throw new Error("Not authenticated");

    const user = await ctx.db
      .query("users")
      .withIndex("by_externalId", (q) => q.eq("externalId", identity.subject))
      .unique();
    if (!user) throw new Error("User not found");

    const sub = await ctx.db
      .query("subscriptions")
      .withIndex("by_deviceId", (q) => q.eq("deviceId", args.deviceId))
      .unique();
    if (!sub) return; // No subscription to link

    await ctx.db.patch(sub._id, { userId: user._id });
  },
});
