const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "Royal Striker",
    email: "alice@example.com",
    avatarId: 1,
    coins: 350,
    highScore: 120,
    careerPoints: 240,
    matchesWon: 4,
    matchesPlayed: 6,
    selectedStrikerId: "classic",
    unlockedStrikerIds: ["classic", "inferno"],
    selectedBoardId: "birch_royal",
    unlockedBoardIds: ["birch_royal"],
    highestChallengeUnlocked: 3,
    completedChallenges: [1, 2],
    lastSpinEpochDay: 20415,
    createdAt: now,
    updatedAt: now,
  };
}

function validMatchPayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    mode: "CLASSIC_1V1",
    playerScore: 85,
    won: true,
    challengeLevel: 0,
    coinsEarned: 111,
    createdAt: now,
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

test("Unauthenticated user cannot read or write user profiles or match records", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("matches").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("matches").doc("m1").set(validMatchPayload(ALICE_UID))
  );
});

test("Authenticated owner can create, read, and partially update their profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(ref.set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(
    ref.update({
      coins: 500,
      selectedStrikerId: "inferno",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user access to profile and PII email is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set(validProfilePayload(BOB_UID));
    await ctx
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("matches")
      .doc("match_1")
      .set(validMatchPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ coins: 9999, updatedAt: new Date(Date.now() - 500) }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("matches").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("matches").doc("match_2").set(validMatchPayload(ALICE_UID))
  );
});

test("Security rules reject @DocumentId 'id', ghost fields, immutable createdAt changes, and invalid timestamps", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Reject explicit `id` field
  const withDocId = { ...validProfilePayload(ALICE_UID), id: ALICE_UID };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(withDocId));

  // Reject ghost/shadow field (`isAdmin: true`)
  const withGhost = { ...validProfilePayload(ALICE_UID), isAdmin: true };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(withGhost));

  // Reject future timestamp (> request.time)
  const futurePayload = validProfilePayload(ALICE_UID, new Date(Date.now() + 60000));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(futurePayload));

  // Valid create, then reject mutating immutable createdAt or userId
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Match history subcollection supports owner create & ordered list query, blocks updates and invalid modes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const matchesCol = aliceDb.collection("users").doc(ALICE_UID).collection("matches");

  // Empty subcollection list succeeds (no resource.data reference in allow list)
  await assertSucceeds(matchesCol.orderBy("createdAt", "desc").limit(20).get());

  await assertSucceeds(matchesCol.doc("m_1").set(validMatchPayload(ALICE_UID)));
  await assertSucceeds(matchesCol.orderBy("createdAt", "desc").limit(20).get());

  // Immutable match records reject updates
  await assertFails(matchesCol.doc("m_1").update({ playerScore: 9999 }));

  // Invalid mode or negative coins fails
  await assertFails(matchesCol.doc("m_2").set({ ...validMatchPayload(ALICE_UID), mode: "HACKED_MODE" }));
  await assertFails(matchesCol.doc("m_3").set({ ...validMatchPayload(ALICE_UID), coinsEarned: -10 }));
});
