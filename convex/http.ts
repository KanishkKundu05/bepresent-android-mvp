import { httpRouter } from "convex/server";
import { httpAction } from "./_generated/server";
import Stripe from "stripe";
import { internal } from "./_generated/api";

const http = httpRouter();

http.route({
  path: "/stripe/create-subscription",
  method: "POST",
  handler: httpAction(async (ctx, request) => {
    const stripe = new Stripe(process.env.STRIPE_SECRET_KEY!, {
      apiVersion: "2023-10-16",
    });
    const priceId = process.env.STRIPE_PRICE_ID!;

    const body = await request.json();
    const { deviceId } = body as { deviceId: string };

    if (!deviceId) {
      return new Response(JSON.stringify({ error: "deviceId is required" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      });
    }

    // Check for existing subscription
    const existing = await ctx.runQuery(
      internal.stripe.getSubscriptionByDevice,
      { deviceId }
    );

    let customerId: string;

    if (existing?.stripeCustomerId) {
      customerId = existing.stripeCustomerId;
    } else {
      // Create new Stripe customer
      const customer = await stripe.customers.create({
        metadata: { deviceId },
      });
      customerId = customer.id;
    }

    // Create subscription with incomplete payment
    const subscription = await stripe.subscriptions.create({
      customer: customerId,
      items: [{ price: priceId }],
      payment_behavior: "default_incomplete",
      payment_settings: {
        save_default_payment_method: "on_subscription",
      },
      expand: ["latest_invoice.payment_intent"],
    });

    const invoice = subscription.latest_invoice as Stripe.Invoice;
    const paymentIntent = invoice.payment_intent as Stripe.PaymentIntent;

    // Upsert subscription in Convex
    await ctx.runMutation(internal.stripe.upsertSubscription, {
      deviceId,
      stripeCustomerId: customerId,
      stripeSubscriptionId: subscription.id,
      status: subscription.status,
      priceId,
      currentPeriodEnd: subscription.current_period_end * 1000,
    });

    return new Response(
      JSON.stringify({
        clientSecret: paymentIntent.client_secret,
        customerId,
        subscriptionId: subscription.id,
      }),
      {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }
    );
  }),
});

http.route({
  path: "/stripe/webhook",
  method: "POST",
  handler: httpAction(async (ctx, request) => {
    const stripe = new Stripe(process.env.STRIPE_SECRET_KEY!, {
      apiVersion: "2023-10-16",
    });
    const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET!;

    const body = await request.text();
    const signature = request.headers.get("stripe-signature");

    if (!signature) {
      return new Response("Missing stripe-signature header", { status: 400 });
    }

    let event: Stripe.Event;
    try {
      event = stripe.webhooks.constructEvent(body, signature, webhookSecret);
    } catch (err) {
      return new Response("Webhook signature verification failed", {
        status: 400,
      });
    }

    switch (event.type) {
      case "invoice.paid": {
        const invoice = event.data.object as Stripe.Invoice;
        if (invoice.subscription) {
          const subId =
            typeof invoice.subscription === "string"
              ? invoice.subscription
              : invoice.subscription.id;
          await ctx.runMutation(internal.stripe.updateSubscriptionStatus, {
            stripeSubscriptionId: subId,
            status: "active",
            currentPeriodEnd: (invoice.lines.data[0]?.period?.end ?? 0) * 1000,
          });
        }
        break;
      }
      case "customer.subscription.updated": {
        const sub = event.data.object as Stripe.Subscription;
        await ctx.runMutation(internal.stripe.updateSubscriptionStatus, {
          stripeSubscriptionId: sub.id,
          status: sub.status,
          currentPeriodEnd: sub.current_period_end * 1000,
        });
        break;
      }
      case "customer.subscription.deleted": {
        const sub = event.data.object as Stripe.Subscription;
        await ctx.runMutation(internal.stripe.updateSubscriptionStatus, {
          stripeSubscriptionId: sub.id,
          status: "canceled",
        });
        break;
      }
    }

    return new Response(JSON.stringify({ received: true }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }),
});

export default http;
