const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validPayload(uid, overrides = {}) {
  const ts = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "YVGPSCAM_20260401_120000.jpg",
    capturedAtMillis: 1775000000000,
    latitude: 37.422094,
    longitude: -122.084057,
    altitudeMeters: 24.5,
    accuracyMeters: 4.2,
    compassAzimuth: 135.0,
    compassCardinal: "SE",
    addressLine: "1600 Amphitheatre Pkwy",
    cityState: "Mountain View, CA",
    country: "USA",
    customNote: "Field Survey A",
    templateName: "TACTICAL_DARK",
    width: 1920,
    height: 1080,
    thumbnailBase64: "dGVzdF9qcGVnX2Jhc2U2NA==",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
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

test("Unauthenticated user: cannot list or create photo_backups", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("photo_backups").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("photo_backups").doc("backup_1").set(validPayload(ALICE_UID))
  );
});

test("Owner: can create, get, list, update note, and delete photo_backups", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("users").doc(ALICE_UID).collection("photo_backups").doc("backup_001");

  await assertSucceeds(docRef.set(validPayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("photo_backups").get());
  await assertSucceeds(
    docRef.update({
      customNote: "Updated site note",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("Cross-user isolation: Bob cannot read, write, update, or delete Alice's backups", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceDoc = aliceDb.collection("users").doc(ALICE_UID).collection("photo_backups").doc("alice_backup");
  await assertSucceeds(aliceDoc.set(validPayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("photo_backups").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("photo_backups").doc("alice_backup").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("photo_backups").doc("bob_spoof").set(validPayload(BOB_UID))
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("photo_backups").doc("alice_backup").update({
      customNote: "Hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("photo_backups").doc("alice_backup").delete()
  );
});

test("Schema & Dirty Dozen guards: rejects ghost fields, identity spoofing, out-of-range coordinates, and immutable field mutation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("photo_backups");

  // Ghost field
  await assertFails(col.doc("b_ghost").set(validPayload(ALICE_UID, { isAdmin: true })));

  // Identity spoofing
  await assertFails(col.doc("b_spoof").set(validPayload(ALICE_UID, { userId: BOB_UID })));

  // Out-of-range latitude
  await assertFails(col.doc("b_lat").set(validPayload(ALICE_UID, { latitude: 120.0 })));

  // Oversized thumbnailBase64 (> 450000 chars)
  await assertFails(col.doc("b_big").set(validPayload(ALICE_UID, { thumbnailBase64: "A".repeat(450001) })));

  // Invalid doc ID
  await assertFails(col.doc("bad id with spaces!").set(validPayload(ALICE_UID)));

  // Immutable userId mutation on update
  const validRef = col.doc("b_valid");
  await assertSucceeds(validRef.set(validPayload(ALICE_UID)));
  await assertFails(validRef.update({ userId: BOB_UID, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(validRef.update({ displayName: "TamperedName.jpg", updatedAt: new Date(Date.now() - 1000) }));
});
