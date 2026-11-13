const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 2000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProduct(ownerId) {
  return {
    ownerId,
    title: "App Lanchonete Express",
    niche: "Lanchonete",
    shortDescription: "Delivery de sanduiches com cardapio digital e PIX",
    fullDescription: "Aplicativo completo para lanchonete com pedidos em tempo real e cupom.",
    price: 1490.0,
    monthlyFee: 149.0,
    stockCount: 15,
    salesCount: 0,
    coverKey: "lanchonete",
    features: ["Cardapio Digital", "Checkout PIX"],
    active: true,
    rating: 4.9,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validUser(userId, role = "cliente") {
  return {
    userId,
    displayName: "Usuario Teste",
    email: "usuario@teste.com",
    role,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validOrder(userId, productId, status = "PENDING") {
  return {
    userId,
    customerName: "Alice Silva",
    customerEmail: "alice@teste.com",
    customerDocument: "12345678900",
    productId,
    productTitle: "App Lanchonete Express",
    niche: "Lanchonete",
    quantity: 1,
    unitPrice: 1490.0,
    totalAmount: 1490.0,
    gatewayFee: 59.45,
    sellerCommission: 149.0,
    netAmount: 1281.55,
    paymentMethod: "PIX",
    paymentStatus: status,
    gatewayProvider: "MERCADO_PAGO",
    gatewayTransactionId: "mp_tx_001",
    installments: 1,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validVisit(userId, status = "AGENDADA") {
  return {
    userId,
    clientName: "Hamburgueria Brasa",
    clientPhone: "(11) 99999-1234",
    clientAddress: "Av. Paulista, 1000",
    niche: "Lanchonete",
    appTitle: "App Lanchonete Express",
    consultantName: "Carlos Consultor",
    scheduledDateIso: "2026-11-20",
    scheduledTime: "14:30",
    visitType: "PRESENCIAL",
    status,
    notes: "Demonstracao do painel de pedidos",
    estimatedValue: 1490.0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validFinancialEntry(userId) {
  return {
    userId,
    title: "Venda App Lanchonete",
    category: "VENDA_APP",
    entryType: "RECEITA",
    profileScope: "ADMIN",
    amount: 1490.0,
    niche: "Lanchonete",
    referenceMonth: "2026-11",
    status: "CONFIRMADO",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validNotification(userId) {
  return {
    userId,
    title: "Black Week Apps Delivery",
    body: "20% de desconto na implantacao do app para sua lanchonete.",
    targetSegment: "Lanchonete",
    campaignType: "PROMOCAO",
    sentBy: "Painel Admin",
    isRead: false,
    dispatchedToDevice: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or list any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("products").get());
  await assertFails(unauth.collection("orders").get());
  await assertFails(unauth.collection("visits").get());
  await assertFails(unauth.collection("financial_entries").get());
  await assertFails(unauth.collection("notifications").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
});

test("User profile: prevents self-assigned admin role and shadow fields", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(alice.collection("users").doc(ALICE_UID).set(validUser(ALICE_UID, "admin")));
  await assertFails(
    alice.collection("users").doc(ALICE_UID).set({
      ...validUser(ALICE_UID, "cliente"),
      isVerified: true,
    })
  );
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).set(validUser(ALICE_UID, "cliente")));
});

test("Products: owner CRUD works with aligned query, cross-user rejected", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(alice.collection("products").doc("prod_1").set(validProduct(ALICE_UID)));
  await assertSucceeds(alice.collection("products").where("ownerId", "==", ALICE_UID).get());

  // Unfiltered query must fail (rules are not filters)
  await assertFails(alice.collection("products").get());

  // Cross-user read, update, delete must fail
  await assertFails(bob.collection("products").doc("prod_1").get());
  await assertFails(bob.collection("products").doc("prod_1").update({ price: 10.0, updatedAt: serverTimestamp() }));
  await assertFails(bob.collection("products").doc("prod_1").delete());

  // Resource poisoning (invalid doc ID)
  await assertFails(alice.collection("products").doc("invalid id!").set(validProduct(ALICE_UID)));
});

test("Orders: requires existing product and enforces terminal state locking", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(alice.collection("products").doc("prod_1").set(validProduct(ALICE_UID)));

  // Orphaned order with non-existent product must fail
  await assertFails(alice.collection("orders").doc("ord_bad").set(validOrder(ALICE_UID, "nonexistent")));

  // Valid order succeeds
  await assertSucceeds(alice.collection("orders").doc("ord_1").set(validOrder(ALICE_UID, "prod_1", "PENDING")));

  // Transition PENDING -> PAID succeeds
  await assertSucceeds(
    alice.collection("orders").doc("ord_1").update({
      paymentStatus: "PAID",
      updatedAt: serverTimestamp(),
    })
  );

  // Terminal state lock: once PAID, non-admin cannot mutate order
  await assertFails(
    alice.collection("orders").doc("ord_1").update({
      paymentStatus: "PENDING",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Visits, Financial Entries, Notifications: owner access works, cross-user blocked", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(alice.collection("visits").doc("vis_1").set(validVisit(ALICE_UID)));
  await assertSucceeds(alice.collection("financial_entries").doc("fin_1").set(validFinancialEntry(ALICE_UID)));
  await assertSucceeds(alice.collection("notifications").doc("not_1").set(validNotification(ALICE_UID)));

  await assertFails(bob.collection("visits").doc("vis_1").get());
  await assertFails(bob.collection("financial_entries").doc("fin_1").get());
  await assertFails(bob.collection("notifications").doc("not_1").get());

  // Terminal visit lock
  await assertSucceeds(
    alice.collection("visits").doc("vis_1").update({ status: "CONCLUIDA", updatedAt: serverTimestamp() })
  );
  await assertFails(
    alice.collection("visits").doc("vis_1").update({ status: "AGENDADA", updatedAt: serverTimestamp() })
  );
});

test("Email spoofing test: unverified admin email is rejected, verified admin succeeds", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(alice.collection("products").doc("prod_alice").set(validProduct(ALICE_UID)));

  const spoofedAdmin = testEnv.authenticatedContext("spoof_uid", {
    email: "acreanamarcia@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofedAdmin.collection("products").doc("prod_alice").get());

  const realAdmin = testEnv.authenticatedContext(ADMIN_UID, {
    email: "acreanamarcia@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(realAdmin.collection("products").doc("prod_alice").get());
});
