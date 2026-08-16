#!/usr/bin/env bash
# Run in Google Cloud Shell (no GitHub clone needed):
#   bash cloudshell-firebase-deploy.sh
set -euo pipefail

ROOT="$HOME/familyos-firebase-deploy"
mkdir -p "$ROOT/firestore"
cd "$ROOT"

cat > firebase.json <<'EOF'
{
  "firestore": {
    "rules": "firestore/firestore.rules"
  },
  "storage": {
    "rules": "firestore/storage.rules"
  }
}
EOF

cat > .firebaserc <<'EOF'
{
  "projects": {
    "default": "segalfamilyos"
  }
}
EOF

cat > firestore/firestore.rules <<'EOF'
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    function isSignedIn() {
      return request.auth != null;
    }

    function uid() {
      return request.auth.uid;
    }

    function memberDoc(familyId) {
      return get(/databases/$(database)/documents/families/$(familyId)/members/$(uid()));
    }

    function isFamilyMember(familyId) {
      return isSignedIn()
        && exists(/databases/$(database)/documents/families/$(familyId)/members/$(uid()));
    }

    function memberRole(familyId) {
      return memberDoc(familyId).data.role;
    }

    function isOwner(familyId) {
      return isFamilyMember(familyId) && memberRole(familyId) == 'OWNER';
    }

    function isAdmin(familyId) {
      return isFamilyMember(familyId)
        && (memberRole(familyId) == 'OWNER' || memberRole(familyId) == 'ADMIN');
    }

    function isFullMember(familyId) {
      return isFamilyMember(familyId)
        && (memberRole(familyId) == 'OWNER'
          || memberRole(familyId) == 'ADMIN'
          || memberRole(familyId) == 'MEMBER');
    }

    function unchanged(field) {
      return !(field in request.resource.data)
        || request.resource.data[field] == resource.data[field];
    }

    match /users/{userId} {
      allow read: if isSignedIn() && uid() == userId;
      allow create: if isSignedIn() && uid() == userId;
      allow update: if isSignedIn() && uid() == userId;
      allow delete: if isSignedIn() && uid() == userId;
    }

    match /families/{familyId} {
      allow read: if isFamilyMember(familyId);
      allow create: if isSignedIn()
        && request.resource.data.ownerId == uid()
        && request.resource.data.keys().hasAll(['name', 'ownerId', 'inviteCode']);
      allow update: if isAdmin(familyId) && unchanged('ownerId');
      allow delete: if isOwner(familyId);

      match /members/{memberId} {
        allow read: if isFamilyMember(familyId);
        allow create: if isSignedIn()
          && (isAdmin(familyId) || (memberId == uid() && request.resource.data.userId == uid()));
        allow update: if isAdmin(familyId)
          || (memberId == uid() && unchanged('role') && unchanged('userId'));
        allow delete: if isAdmin(familyId)
          || (memberId == uid() && !isOwner(familyId));
      }
    }

    match /members/{memberId} {
      allow read: if isSignedIn() && (
        resource.data.userId == uid()
        || (resource.data.familyId is string && isFamilyMember(resource.data.familyId))
      );
      allow create, update: if isSignedIn()
        && request.resource.data.familyId is string
        && (isAdmin(request.resource.data.familyId) || request.resource.data.userId == uid());
      allow delete: if isSignedIn()
        && resource.data.familyId is string
        && (isAdmin(resource.data.familyId) || resource.data.userId == uid());
    }

    match /shopping/{itemId} {
      allow read: if isFamilyMember(resource.data.familyId);
      allow create: if isFullMember(request.resource.data.familyId)
        && request.resource.data.createdBy == uid();
      allow update: if isFullMember(resource.data.familyId);
      allow delete: if isAdmin(resource.data.familyId)
        || (isFullMember(resource.data.familyId) && resource.data.createdBy == uid());
    }

    match /tasks/{taskId} {
      allow read: if isFamilyMember(resource.data.familyId);
      allow create: if isFullMember(request.resource.data.familyId)
        && request.resource.data.createdBy == uid();
      allow update: if isFullMember(resource.data.familyId);
      allow delete: if isAdmin(resource.data.familyId)
        || (isFullMember(resource.data.familyId) && resource.data.createdBy == uid());
    }

    match /events/{eventId} {
      allow read: if isFamilyMember(resource.data.familyId);
      allow create: if isFullMember(request.resource.data.familyId);
      allow update: if isFullMember(resource.data.familyId);
      allow delete: if isAdmin(resource.data.familyId)
        || (isFullMember(resource.data.familyId) && resource.data.createdBy == uid());
    }

    match /budgets/{txId} {
      allow read: if isFamilyMember(resource.data.familyId);
      allow create: if isFullMember(request.resource.data.familyId)
        && request.resource.data.createdBy == uid();
      allow update: if isAdmin(resource.data.familyId)
        || (isFullMember(resource.data.familyId) && resource.data.createdBy == uid());
      allow delete: if isAdmin(resource.data.familyId);
    }

    match /documents/{docId} {
      allow read: if isFamilyMember(resource.data.familyId);
      allow create: if isFullMember(request.resource.data.familyId)
        && request.resource.data.uploadedBy == uid();
      allow update: if isAdmin(resource.data.familyId)
        || (isFullMember(resource.data.familyId) && resource.data.uploadedBy == uid());
      allow delete: if isAdmin(resource.data.familyId)
        || (isFullMember(resource.data.familyId) && resource.data.uploadedBy == uid());
    }

    match /notes/{noteId} {
      allow read: if isFamilyMember(resource.data.familyId);
      allow create: if isFullMember(request.resource.data.familyId)
        && request.resource.data.createdBy == uid();
      allow update: if isFullMember(resource.data.familyId);
      allow delete: if isAdmin(resource.data.familyId)
        || (isFullMember(resource.data.familyId) && resource.data.createdBy == uid());
    }

    match /chat/{threadId} {
      allow read: if isFamilyMember(resource.data.familyId);
      allow create: if isFullMember(request.resource.data.familyId)
        && request.resource.data.createdBy == uid();
      allow update: if isFullMember(resource.data.familyId);
      allow delete: if isAdmin(resource.data.familyId);

      match /messages/{messageId} {
        allow read: if isFamilyMember(get(/databases/$(database)/documents/chat/$(threadId)).data.familyId);
        allow create: if isFullMember(request.resource.data.familyId)
          && request.resource.data.senderId == uid()
          && request.resource.data.threadId == threadId;
        allow update: if isFullMember(resource.data.familyId)
          && (
            resource.data.senderId == uid()
            || request.resource.data.diff(resource.data).affectedKeys().hasOnly(['readBy', 'updatedAt'])
          );
        allow delete: if isAdmin(resource.data.familyId) || resource.data.senderId == uid();
      }
    }

    match /messages/{messageId} {
      allow read: if isFamilyMember(resource.data.familyId);
      allow create: if isFullMember(request.resource.data.familyId)
        && request.resource.data.senderId == uid();
      allow update: if isFullMember(resource.data.familyId)
        && (
          resource.data.senderId == uid()
          || request.resource.data.diff(resource.data).affectedKeys().hasOnly(['readBy', 'updatedAt'])
        );
      allow delete: if isAdmin(resource.data.familyId) || resource.data.senderId == uid();
    }

    match /notifications/{notificationId} {
      allow read: if isSignedIn() && resource.data.userId == uid();
      allow create: if isSignedIn()
        && (
          request.resource.data.userId == uid()
          || (request.resource.data.familyId is string && isFullMember(request.resource.data.familyId))
        );
      allow update: if isSignedIn() && resource.data.userId == uid();
      allow delete: if isSignedIn() && resource.data.userId == uid();
    }

    match /ai_history/{conversationId} {
      allow read: if isSignedIn()
        && resource.data.userId == uid()
        && isFamilyMember(resource.data.familyId);
      allow create: if isSignedIn()
        && request.resource.data.userId == uid()
        && isFullMember(request.resource.data.familyId);
      allow update: if isSignedIn()
        && resource.data.userId == uid()
        && isFamilyMember(resource.data.familyId);
      allow delete: if isSignedIn() && resource.data.userId == uid();
    }

    match /{document=**} {
      allow read, write: if false;
    }
  }
}
EOF

cat > firestore/storage.rules <<'EOF'
rules_version = '2';

service firebase.storage {
  match /b/{bucket}/o {

    function isSignedIn() {
      return request.auth != null;
    }

    function uid() {
      return request.auth.uid;
    }

    function isImage() {
      return request.resource != null
        && request.resource.contentType.matches('image/.*');
    }

    function isAllowedDocument() {
      return request.resource != null
        && (
          request.resource.contentType == 'application/pdf'
          || request.resource.contentType == 'application/msword'
          || request.resource.contentType == 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
          || request.resource.contentType.matches('image/jpeg')
          || request.resource.contentType.matches('image/png')
          || request.resource.contentType.matches('image/webp')
          || request.resource.contentType.matches('audio/.*')
        );
    }

    function underMaxSize(maxBytes) {
      return request.resource != null && request.resource.size < maxBytes;
    }

    function isFamilyMember(familyId) {
      return isSignedIn()
        && firestore.exists(/databases/(default)/documents/families/$(familyId)/members/$(uid()));
    }

    function memberRole(familyId) {
      return firestore.get(/databases/(default)/documents/families/$(familyId)/members/$(uid())).data.role;
    }

    function isFullMember(familyId) {
      return isFamilyMember(familyId)
        && (
          memberRole(familyId) == 'OWNER'
          || memberRole(familyId) == 'ADMIN'
          || memberRole(familyId) == 'MEMBER'
        );
    }

    function isAdmin(familyId) {
      return isFamilyMember(familyId)
        && (memberRole(familyId) == 'OWNER' || memberRole(familyId) == 'ADMIN');
    }

    match /documents/{familyId}/{userId}/{fileName} {
      allow read: if isFamilyMember(familyId);
      allow create: if isFullMember(familyId)
        && userId == uid()
        && isAllowedDocument()
        && underMaxSize(25 * 1024 * 1024);
      allow update: if isFullMember(familyId) && userId == uid();
      allow delete: if isAdmin(familyId) || (isFullMember(familyId) && userId == uid());
    }

    match /attachments/{familyId}/{userId}/{fileName} {
      allow read: if isFamilyMember(familyId);
      allow create: if isFullMember(familyId)
        && userId == uid()
        && underMaxSize(20 * 1024 * 1024);
      allow update: if isFullMember(familyId) && userId == uid();
      allow delete: if isAdmin(familyId) || (isFullMember(familyId) && userId == uid());
    }

    match /avatars/{userId}/{fileName} {
      allow read: if isSignedIn();
      allow write: if isSignedIn()
        && userId == uid()
        && isImage()
        && underMaxSize(5 * 1024 * 1024);
    }

    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
EOF

echo "Files ready in $ROOT"
npx --yes firebase-tools deploy --only firestore:rules,storage --project segalfamilyos
echo "DONE"
