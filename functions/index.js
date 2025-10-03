const functions = require("firebase-functions");
const admin = require("firebase-admin");
if (!admin.apps.length) admin.initializeApp();

exports.cleanupExpiredRequests = functions.pubsub
  .schedule("every 60 minutes")
  .timeZone("UTC")
  .onRun(async () => {
    const db = admin.database();
    const now = Date.now();
    const pubRef = db.ref("requests_public");
    const pubSnap = await pubRef.get();

    const toDelete = [];

    pubSnap.forEach(child => {
      const needed = child.child("neededOnMillis").val() || 0;
      if (needed > 0 && needed < now) {
        const id = child.key;
        const ownerUid = child.child("ownerUid").val();
        // Remove from public
        toDelete.push(pubRef.child(id).remove());
        // Remove from owner-private copy too
        if (ownerUid) {
          toDelete.push(db.ref(`requests/${ownerUid}/${id}`).remove());
        }
      }
    });

    await Promise.allSettled(toDelete);
    console.log(`cleanupExpiredRequests done: ${toDelete.length} ops`);
    return null;
  });
