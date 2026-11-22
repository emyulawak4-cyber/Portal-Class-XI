const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CLASS_ID = "class_xi_puspita";
const ALICE_UID = "alice_uid_01";
const BOB_UID = "bob_uid_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

async function seedClassRoot() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const ts = new Date(Date.now() - 10000);
    await db.collection("classes").doc(CLASS_ID).set({
      classId: CLASS_ID,
      userId: ALICE_UID,
      name: "CLASS XI",
      school: "SMA Puspita Kota Tangerang",
      kasPeriods: [
        "Minggu 1 - Nov 2026",
        "Minggu 2 - Nov 2026",
        "Minggu 3 - Nov 2026",
        "Minggu 4 - Nov 2026",
      ],
      createdAt: ts,
      updatedAt: ts,
    });
  });
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

test("Unauthenticated users cannot read or write any collection", async () => {
  await seedClassRoot();
  const unauth = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);

  await assertFails(unauth.collection("classes").doc(CLASS_ID).get());
  await assertFails(
    unauth.collection("announcements").where("classId", "==", CLASS_ID).get()
  );
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      notifyEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Users collection isolates PII strictly to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Adara",
      email: "adara@example.com",
      studentId: 1,
      notifyEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot read or overwrite Alice's PII
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Spoofed",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject future timestamp or immutable field tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future",
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});

test("Class bootstrap, announcements, and friend notifications obey schema & query filters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("classes").doc(CLASS_ID).set({
      classId: CLASS_ID,
      userId: ALICE_UID,
      name: "CLASS XI",
      school: "SMA Puspita Kota Tangerang",
      kasPeriods: ["Minggu 1 - Nov 2026"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject @DocumentId `id` key on create
  await assertFails(
    aliceDb.collection("announcements").doc("ann_bad").set({
      id: "ann_bad",
      classId: CLASS_ID,
      userId: ALICE_UID,
      title: "Bad",
      category: "Penting",
      deadline: "Hari Ini",
      desc: "Should fail",
      author: "Ketua Kelas",
      dateLabel: "22 Nov",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid announcement + friend notification event
  await assertSucceeds(
    aliceDb.collection("announcements").doc("ann_1").set({
      classId: CLASS_ID,
      userId: ALICE_UID,
      title: "Praktikum Kimia Besok",
      category: "Penting",
      deadline: "Besok 13:00",
      desc: "Bawa jas lab masing-masing.",
      author: "Ketua Kelas",
      dateLabel: "22 Nov • 13:00",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("class_notifications").doc("notif_1").set({
      classId: CLASS_ID,
      userId: ALICE_UID,
      senderName: "Ketua Kelas",
      title: "[Penting] Praktikum Kimia Besok",
      body: "Bawa jas lab masing-masing.",
      category: "info",
      targetTab: "info",
      createdAt: ts,
    })
  );

  // Filtered friend query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("announcements").where("classId", "==", CLASS_ID).get()
  );
  await assertSucceeds(
    bobDb.collection("class_notifications").where("classId", "==", CLASS_ID).get()
  );
  await assertFails(bobDb.collection("announcements").get());
  await assertFails(bobDb.collection("class_notifications").get());

  // Bob cannot delete Alice's announcement
  await assertFails(bobDb.collection("announcements").doc("ann_1").delete());
});

test("Leave requests enforce Menunggu on create, terminal state lock, and attendance sync", async () => {
  await seedClassRoot();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Cannot pre-approve own leave request
  await assertFails(
    aliceDb.collection("leave_requests").doc("leave_bad").set({
      classId: CLASS_ID,
      userId: ALICE_UID,
      studentId: 1,
      noAbsen: "01",
      studentName: "Adara Adzra Aprillia",
      piket: "Senin",
      date: "2026-11-22",
      type: "Sakit",
      reason: "Demam",
      gformConfirmed: true,
      status: "Disetujui",
      submittedAt: "2026-11-22 • 13:00 WIB",
      verifiedBy: "Self",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("leave_requests").doc("leave_1").set({
      classId: CLASS_ID,
      userId: ALICE_UID,
      studentId: 1,
      noAbsen: "01",
      studentName: "Adara Adzra Aprillia",
      piket: "Senin",
      date: "2026-11-22",
      type: "Sakit",
      reason: "Demam dan flu",
      gformConfirmed: true,
      status: "Menunggu",
      submittedAt: "2026-11-22 • 13:00 WIB",
      verifiedBy: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob (pengurus) approves leave_1
  await assertSucceeds(
    bobDb.collection("leave_requests").doc("leave_1").update({
      status: "Disetujui",
      verifiedBy: "Sekretaris 1",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state locking: cannot modify status after Disetujui
  await assertFails(
    bobDb.collection("leave_requests").doc("leave_1").update({
      status: "Ditolak",
      verifiedBy: "Sekretaris 2",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Attendance day create & partial update
  await assertSucceeds(
    bobDb.collection("attendance_days").doc("2026-11-22").set({
      classId: CLASS_ID,
      userId: BOB_UID,
      dateKey: "2026-11-22",
      records: { "1": { status: "Sakit", scanTime: "" } },
      lastUpdatedBy: "Sekretaris 1",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("attendance_days").doc("2026-11-22").update({
      records: {
        "1": { status: "Sakit", scanTime: "" },
        "2": { status: "Hadir", scanTime: "12:55" },
      },
      lastUpdatedBy: "Ketua Kelas",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Dokumentasi atomic like/unlike batch & comments & materi & kas rules", async () => {
  await seedClassRoot();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("dokumentasi").doc("dok_1").set({
      classId: CLASS_ID,
      userId: ALICE_UID,
      title: "Praktikum Fisika",
      category: "Praktikum",
      mediaType: "photo",
      mediaUrl: "https://example.com/photo.jpg",
      fileName: "fisika.jpg",
      caption: "Seru banget!",
      author: "Sekretaris 1",
      date: "2026-11-22",
      likesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-atomic like count manipulation without /likes/{uid} batch must FAIL
  await assertFails(
    bobDb.collection("dokumentasi").doc("dok_1").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Atomic batch like by Bob succeeds
  const likeBatch = bobDb.batch();
  likeBatch.set(
    bobDb.collection("dokumentasi").doc("dok_1").collection("likes").doc(BOB_UID),
    { classId: CLASS_ID, userId: BOB_UID, createdAt: ts }
  );
  likeBatch.update(bobDb.collection("dokumentasi").doc("dok_1"), {
    likesCount: 1,
    updatedAt: new Date(Date.now() - 1000),
  });
  await assertSucceeds(likeBatch.commit());

  // Comment create & query
  await assertSucceeds(
    bobDb.collection("dokumentasi").doc("dok_1").collection("comments").doc("c1").set({
      classId: CLASS_ID,
      dokId: "dok_1",
      userId: BOB_UID,
      senderName: "Siswa CLASS XI",
      text: "Mantap!",
      timeLabel: "22 Nov • 13:15",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb
      .collection("dokumentasi")
      .doc("dok_1")
      .collection("comments")
      .where("classId", "==", CLASS_ID)
      .get()
  );

  // Kas transaction & period
  await assertSucceeds(
    aliceDb.collection("kas_transactions").doc("tx_1").set({
      classId: CLASS_ID,
      userId: ALICE_UID,
      type: "Pemasukan",
      amount: 5000,
      date: "2026-11-22",
      category: "Iuran Kas Siswa",
      note: "Pembayaran Kas Minggu 1",
      author: "Bendahara1",
      studentId: 1,
      period: "Minggu 1 - Nov 2026",
      voided: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("kas_transactions").doc("tx_1").update({
      voided: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Materi create & download increment
  await assertSucceeds(
    aliceDb.collection("materi").doc("mat_1").set({
      classId: CLASS_ID,
      userId: ALICE_UID,
      title: "Modul Turunan",
      guru: "Pak Budi",
      mapel: "Matematika (MTK)",
      desc: "Ringkasan rumus",
      dateLabel: "22 Nov 2026",
      dl: 0,
      fileName: "turunan.pdf",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("materi").doc("mat_1").update({
      dl: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
